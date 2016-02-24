package io.dazraf.vertx.deployer;

import io.dazraf.vertx.HotDeployParameters;
import io.dazraf.vertx.web.WebNotificationService;
import io.vertx.core.AsyncResult;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.impl.VertxWrapper;
import io.vertx.core.json.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rx.exceptions.Exceptions;
import rx.functions.Action1;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import static io.dazraf.vertx.deployer.DeploymentResult.failure;
import static io.dazraf.vertx.deployer.DeploymentResult.success;

public class GenericVerticleDeployer implements VerticleDeployer {
  private static final Logger logger = LoggerFactory.getLogger(GenericVerticleDeployer.class);
  private final Vertx vertx;
  private final Set<String> verticleReferences;
  private final Optional<String> vertxConfigFilePath;
  private final AtomicLong nextIsolationGroup = new AtomicLong(1);

  static {
    // We set this property to prevent Vert.x caching files loaded from the classpath on disk
    // This means if you edit the template files in your IDE then the next time they are served the new ones will
    // be served without you having to restart the main()
    System.setProperty("vertx.disableFileCaching", "true");
  }

  public GenericVerticleDeployer(HotDeployParameters hotDeployParameters) {
    this(hotDeployParameters, Optional.empty());
  }

  GenericVerticleDeployer(HotDeployParameters hotDeployParameters, Optional<Vertx> vertx) {
    this.vertx = vertx.orElseGet(() -> {
      if (hotDeployParameters.isLiveHttpReload()) {
        return new VertxWrapper(new VertxOptions().setBlockedThreadCheckInterval(3_600_000));
      } else {
        try {
         return Vertx.vertx();
        } catch (Throwable t) {
          logger.error("Failed to start Vertx", t);
          throw t;
        }
      }
    });
    if (hotDeployParameters.isLiveHttpReload()) {
      this.vertx.deployVerticle(new WebNotificationService(hotDeployParameters.getNotificationPort()));
    }
    this.verticleReferences = hotDeployParameters.getVerticleReferences();
    this.vertxConfigFilePath = hotDeployParameters.getConfigFilePath();
  }

  public Action1<JsonObject> createStatusConsumer() {
    return (status) -> vertx.eventBus().publish(WebNotificationService.TOPIC, status);
  }

  public void close() {
    CountDownLatch latch = new CountDownLatch(1);
    vertx.close(ar -> {
      if (ar.failed()) {
        logger.error("error during shutting down vertx", ar.cause());
      }
      latch.countDown();
    });
    try {
      latch.await();
    } catch (InterruptedException e) {
      logger.error("error during shutting down vertx", e);
    }
  }

  public Closeable deploy(List<String> classPaths) throws Throwable {
    DeploymentOptions deploymentOptions = createIsolatingDeploymentOptions(classPaths, vertxConfigFilePath);
    final List<DeploymentResult> deploymentResults = verticleReferences.stream()
      .map(vr -> deployVerticle(vr, deploymentOptions))
      .collect(Collectors.toList());
    logDeploymentReport(deploymentResults);
    return () -> {
      try {
        List<String> verticleIds = deploymentResults.stream()
          .filter(DeploymentResult::isSuccess)
          .map(result -> result.getVerticleId().get())
          .collect(Collectors.toList());
        CountDownLatch closeLatch = new CountDownLatch(verticleIds.size());
        verticleIds.stream()
          .forEach(id -> vertx.undeploy(id, ar -> closeLatch.countDown()));
        closeLatch.await();
      } catch (Exception e) {
        logger.error("on closing verticle", e);
      }
    };
  }

  private void logDeploymentReport(List<DeploymentResult> deploymentResults) {
    deploymentResults.stream()
      .sorted((d1, d2) -> d1.isSuccess() && !d2.isSuccess() ?
          1 : !d1.isSuccess() && d2.isSuccess() ?
            -1 : 0)
      .map(DeploymentResult::toString)
      .forEach(logger::info);
  }

  private DeploymentResult deployVerticle(String verticleReference, DeploymentOptions deploymentOptions) {
    try {
      CountDownLatch latch = new CountDownLatch(1);
      AtomicReference<AsyncResult<String>> result = new AtomicReference<>();
      vertx.deployVerticle(verticleReference, deploymentOptions, ar -> {
        result.set(ar);
        latch.countDown();
      });
      try {
        latch.await();
      } catch (InterruptedException e) {
        Exceptions.propagate(e);
      }
      if (result.get().failed()) {
        Exceptions.throwIfFatal(result.get().cause());
        return failure(verticleReference, result.get().cause());
      }
      return success(verticleReference, result.get().result());
    } catch (Error err) {
      // Vertx throws a generic java.lang.Error on Verticle compilation failure
      Exceptions.throwIfFatal(err);
      logger.error("on compiling verticle {}", verticleReference, err);
      return failure(verticleReference, err);
    }
  }

  private DeploymentOptions createIsolatingDeploymentOptions(List<String> classPaths, Optional<String> config) throws IOException {
    DeploymentOptions result = new DeploymentOptions()
      .setExtraClasspath(classPaths)
      .setIsolationGroup(Long.toString(nextIsolationGroup.getAndIncrement()))
      .setIsolatedClasses(Collections.singletonList("*"));
    return assignConfig(classPaths, config, result);
  }

  private DeploymentOptions assignConfig(List<String> classPaths, Optional<String> config, DeploymentOptions deploymentOptions) throws IOException {
    JsonObject jsonConfig;
    if (config.isPresent()) {
      jsonConfig = loadConfig(classPaths, config.get());
    } else {
      jsonConfig = new JsonObject();
    }
    jsonConfig.put("devmode", true);
    deploymentOptions.setConfig(jsonConfig);
    return deploymentOptions;
  }

  private JsonObject loadConfig(List<String> classPath, String configFile) throws IOException {
    URL[] urls = classPath.stream().map(p -> {
      try {
        return new File(p).toURI().toURL();
      } catch (MalformedURLException e) {
        throw new RuntimeException("error creating URL from path: " + p, e);
      }
    }).toArray(URL[]::new);
    try (URLClassLoader classLoader = new URLClassLoader(urls)) {
      try (InputStream resourceAsStream = classLoader.getResourceAsStream(configFile)) {
        try (Scanner scanner = new Scanner(resourceAsStream, "UTF-8")) {
          String config = scanner.useDelimiter("\\A").next();
          return new JsonObject(config);
        }
      }
    }
  }
}

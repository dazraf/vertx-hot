package io.dazraf.vertx.maven.deployer;

import io.dazraf.vertx.maven.web.WebNotificationService;
import io.vertx.core.AsyncResult;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.eventbus.MessageProducer;
import io.vertx.core.impl.VertxWrapper;
import io.vertx.core.json.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Scanner;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

public class VerticleDeployer implements Closeable {
  private static final Logger logger = LoggerFactory.getLogger(VerticleDeployer.class);
  private final Vertx vertx;
  private final AtomicLong nextIsolationGroup = new AtomicLong(1);

  static {
    // We set this property to prevent Vert.x caching files loaded from the classpath on disk
    // This means if you edit the template files in your IDE then the next time they are served the new ones will
    // be served without you having to restart the main()
    System.setProperty("vertx.disableFileCaching", "true");
  }

  public VerticleDeployer(boolean hotHttpServer, int notificationPort) {
    if (hotHttpServer) {
      this.vertx = new VertxWrapper(new VertxOptions().setBlockedThreadCheckInterval(3_600_000));
      vertx.deployVerticle(new WebNotificationService(notificationPort));
    } else {
      this.vertx = Vertx.vertx();
    }
  }

  public MessageProducer<JsonObject> createEventProducer() {
    return vertx.eventBus().publisher(WebNotificationService.TOPIC);
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

  public Closeable deploy(String verticleReference, List<String> classPaths, Optional<String> config) throws Throwable {
    DeploymentOptions deploymentOptions = createIsolatingDeploymentOptions(classPaths, config);
    final String verticleId = deployVerticle(verticleReference, deploymentOptions);
    return verticleId == null ? null : () -> {
      try {
        CountDownLatch closeLatch = new CountDownLatch(1);
        vertx.undeploy(verticleId, ar -> closeLatch.countDown());
        closeLatch.await();
      } catch (Exception e) {
        logger.error("on closing verticle", e);
      }
    };
  }

  private String deployVerticle(String verticleReference, DeploymentOptions deploymentOptions) throws Throwable {
    try {
      CountDownLatch latch = new CountDownLatch(1);
      AtomicReference<AsyncResult<String>> result = new AtomicReference<>();
      vertx.deployVerticle(verticleReference, deploymentOptions, ar -> {
        result.set(ar);
        latch.countDown();
      });
      latch.await();
      if (result.get().failed()) {
        throw result.get().cause();
      }
      return result.get().result();
    } catch (Error err) {
      // Vertx throws a generic java.lang.Error on Verticle compilation failure
      if (!(err instanceof VirtualMachineError)) {
        logger.error("on compiling verticle {}", verticleReference, err);
      }
      throw err;
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
    if (config.isPresent()) {
      JsonObject jsonConfig = loadConfig(classPaths, config.get());
      jsonConfig.put("devmode", true);
      deploymentOptions.setConfig(jsonConfig);
    }
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

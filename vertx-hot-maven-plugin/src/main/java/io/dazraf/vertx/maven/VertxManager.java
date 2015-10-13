package io.dazraf.vertx.maven;

import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Scanner;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

public class VertxManager implements Closeable {
  private static final Logger logger = LoggerFactory.getLogger(VertxManager.class);
  private final Vertx vertx = Vertx.vertx();
  private AtomicLong nextIsolationGroup = new AtomicLong(1);

  static {
    // We set this property to prevent Vert.x caching files loaded from the classpath on disk
    // This means if you edit the static files in your IDE then the next time they are served the new ones will
    // be served without you having to restart the main()
    // This is only useful for development - do not use this in a production server
    System.setProperty("vertx.disableFileCaching", "true");
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

  public Closeable deploy(String verticleClassName, List<String> classPaths, Optional<String> config) throws Exception {
    DeploymentOptions deploymentOptions = new DeploymentOptions()
      .setExtraClasspath(classPaths)
      .setIsolationGroup(Long.toString(nextIsolationGroup.getAndIncrement()))
      .setIsolatedClasses(Arrays.asList("*"));
    if (config.isPresent()) {
      JsonObject jsonConfig = loadConfig(classPaths, config.get());
      deploymentOptions.setConfig(jsonConfig);
    }
    AtomicReference<String> verticleId = new AtomicReference<>();
    CountDownLatch latch = new CountDownLatch(1);
    vertx.deployVerticle(verticleClassName, deploymentOptions, ar -> {
      verticleId.set(ar.result());
      latch.countDown();
    });
    latch.await();
    return () -> {
      try {
        CountDownLatch closeLatch = new CountDownLatch(1);
        vertx.undeploy(verticleId.get(), ar -> closeLatch.countDown());
        closeLatch.await();
      } catch (Exception e) {
        logger.error("on closing verticle", e);
      }
    };
  }

  private JsonObject loadConfig(List<String> classPath, String configFile) throws IOException {
    URL[] urls = classPath.stream().map(p -> {
      try {
        return new File(p).toURI().toURL();
      } catch (MalformedURLException e) {
        throw new RuntimeException("error creating URL from path: " + p, e);
      }
    }).toArray(URL[]::new);
    URLClassLoader classLoader = new URLClassLoader(urls);
    try {
      try (InputStream resourceAsStream = classLoader.getResourceAsStream(configFile)) {
        String config = new Scanner(resourceAsStream, "UTF-8").useDelimiter("\\A").next();
        return new JsonObject(config);
      }
    } finally {
      classLoader.close();
    }
  }
}

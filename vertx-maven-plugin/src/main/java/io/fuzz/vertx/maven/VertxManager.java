package io.fuzz.vertx.maven;

import com.google.common.base.Charsets;
import com.google.common.io.Resources;
import io.vertx.core.AsyncResult;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.json.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.stream.Collectors;

public class VertxManager implements Closeable {
  private static final Logger logger = LoggerFactory.getLogger(VertxManager.class);
  static {
    // We set this property to prevent Vert.x caching files loaded from the classpath on disk
    // This means if you edit the static files in your IDE then the next time they are served the new ones will
    // be served without you having to restart the main()
    // This is only useful for development - do not use this in a production server
    System.setProperty("vertx.disableFileCaching", "true");
  }


  public void close() {
  }

  public Closeable deploy(String verticleClassName, List<String> classPaths) {
    final CountDownLatch latch = new CountDownLatch(1);
    final Thread thread = new Thread(createRunnable(classPaths, verticleClassName, latch));
    thread.start();
    return () -> {
      latch.countDown();
      try {
        thread.join();
      } catch (InterruptedException e) {
        throw new RuntimeException(e);
      }
    };
  }

  private Runnable createRunnable(List<String> classPaths, String verticleClassName, CountDownLatch latch) {
    URL[] urls = classPaths.stream().map(p -> {
      try {
        return new File(p).toURI().toURL();
      } catch (Exception e) {
        logger.error("problem with: " + p, e);
        throw new RuntimeException(e);
      }
    }).collect(Collectors.toList()).toArray(new URL[]{});

    final URLClassLoader urlClassLoader = new URLClassLoader(urls);
    return () -> {
      try {
        Thread.currentThread().setContextClassLoader(urlClassLoader);
        Class vertxClass = loadClass(Vertx.class.getCanonicalName());
        Object vertx = vertxClass.getMethod("vertx").invoke(null);
        Method deployVerticle = vertxClass.getMethod("deployVerticle", String.class);

        JsonObject config = loadConfig(classPaths);
        deployVerticle.invoke(vertx, verticleClassName);
        logger.info("verticle deployed");
        latch.await();
        vertxClass.getMethod("close").invoke(vertx);
      } catch (Exception e) {
        logger.error("Error starting verticle", e);
        throw new RuntimeException(e);
      }
    };
  }

  private JsonObject loadConfig(List<String> classPaths) throws IOException {
    URL[] urls = classPaths.stream().map(s -> {
      try {
        return new File(s).toURI().toURL();
      } catch (MalformedURLException e) {
        throw new RuntimeException(e);
      }
    }).toArray(URL[]::new);

    try (URLClassLoader classLoader = new URLClassLoader(urls)) {
      URL resource = classLoader.getResource("conf.json");
      if (resource == null) {
        logger.error("Failed to load config file");
        throw new IOException("Failed to load config file");
      }
      return new JsonObject(Resources.toString(resource, Charsets.UTF_8));
    }
  }


  private <T> Class<T> loadClass(String name) throws ClassNotFoundException {
    return (Class<T>) Thread
        .currentThread()
        .getContextClassLoader()
        .loadClass(name);
  }
}

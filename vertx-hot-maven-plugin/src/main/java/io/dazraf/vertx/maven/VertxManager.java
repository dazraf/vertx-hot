package io.dazraf.vertx.maven;

import com.google.common.base.Charsets;
import com.google.common.io.Resources;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;

import static io.dazraf.vertx.maven.Utils.*;

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

  public Closeable deploy(String verticleClassName, List<String> classPaths, Optional<String> config) {
    final CountDownLatch latch = new CountDownLatch(1);

    final Thread thread = new Thread(
      createRunnable(classPaths, verticleClassName, config, latch));

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

  /**
   * Return a runnable that when executed:
   * - replaces the thread context's class loader with the executables classpath
   * - starts vertx
   * - deploys the request verticle, with the specific config file
   * - waits until signalled
   * - tears down the vertx instance
   *
   * @param classPaths the list of paths for the executable
   * @param verticleClassName the name of the main verticle
   * @param config optional config file name
   * @param latch reaches zero when its time to close the vertx instance
   * @return
   */
  private Runnable createRunnable(List<String> classPaths, String verticleClassName, Optional<String> config, CountDownLatch latch) {
    URL[] urls = classPaths.stream().map(p -> {
      try {
        return new File(p).toURI().toURL();
      } catch (Exception e) {
        logger.error("problem with: " + p, e);
        throw new RuntimeException(e);
      }
    }).toArray(URL[]::new);

    final URLClassLoader urlClassLoader = new URLClassLoader(urls);

    return () -> {
      try {
        // bind the new class loader to this thread
        Thread.currentThread().setContextClassLoader(urlClassLoader);

        Class<Vertx> vertxClass = loadClassFromContextClassLoader(Vertx.class);
        Object vertx = createVertx(vertxClass);

        if (config.isPresent()) {
          deployVerticleWithConfig(verticleClassName, urlClassLoader, vertxClass, vertx);
        } else {
          deployVerticleWithNoConfig(verticleClassName, vertxClass, vertx);
        }
        logger.info("verticle deployed");
        latch.await();
        closeVertx(vertxClass, vertx);
      } catch (Exception e) {
        logger.error("Error starting verticle", e);
        throw new RuntimeException(e);
      }
    };
  }

  private Object createVertx(Class<Vertx> vertxClass) throws IllegalAccessException, InvocationTargetException, NoSuchMethodException {
    return getVertxFactoryMethod(vertxClass).invoke(null);
  }

  private void closeVertx(Class<Vertx> vertxClass, Object vertx) throws IllegalAccessException, InvocationTargetException, NoSuchMethodException {
    getVertxCloseMethod(vertxClass).invoke(vertx);
  }

  private void deployVerticleWithNoConfig(String verticleClassName, Class<Vertx> vertxClass, Object vertx) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
    Method deployVerticle = getDeployVerticleMethodWithNoOptions(vertxClass);
    deployVerticle.invoke(vertx, verticleClassName);
  }

  private void deployVerticleWithConfig(String verticleClassName, URLClassLoader urlClassLoader, Class<Vertx> vertxClass, Object vertx) throws Exception {
    Class<DeploymentOptions> deploymentOptionsClass = loadClassFromContextClassLoader(DeploymentOptions.class);
    Object deploymentOptions = deploymentOptionsClass.newInstance();

    Object jsonConfig = loadConfig(urlClassLoader);
    getDeploymentOptionsSetConfigMethod(deploymentOptionsClass).invoke(deploymentOptions, jsonConfig);
    Method deployVerticle = getDeployVerticleMethodWithOptions(vertxClass, deploymentOptionsClass);
    deployVerticle.invoke(vertx, verticleClassName, deploymentOptions);
  }

  private Object loadConfig(ClassLoader classLoader) throws Exception {
    URL resource = classLoader.getResource("conf.json");
    if (resource == null) {
      logger.error("Failed to load config file");
      throw new IOException("Failed to load config file");
    }
    String config = Resources.toString(resource, Charsets.UTF_8);
    return loadClassFromContextClassLoader(JsonObject.class)
      .getConstructor(String.class)
      .newInstance(config);
  }
}

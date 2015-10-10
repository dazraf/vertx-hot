package io.dazraf.vertx.maven;

import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;

public class Utils {
  private final static Logger logger = LoggerFactory.getLogger(Utils.class);

  public static String getCWD() {
    return System.getProperty("user.dir");
  }

  @SuppressWarnings("unchecked")
  public static <T> Class<T> loadClassFromContextClassLoader(Class<T> clazz) throws ClassNotFoundException {
    return (Class<T>) Thread
      .currentThread()
      .getContextClassLoader()
      .loadClass(clazz.getCanonicalName());
  }

  public static Method getDeploymentOptionsSetConfigMethod(Class<DeploymentOptions> deploymentOptionsClass) throws ReflectiveOperationException {
    return deploymentOptionsClass.getMethod("setConfig", loadClassFromContextClassLoader(JsonObject.class));
  }

  public static Method getDeployVerticleMethodWithOptions(Class<Vertx> vertxClass, Class<DeploymentOptions> deploymentOptionsClass) throws NoSuchMethodException {
    return vertxClass.getMethod("deployVerticle", String.class, deploymentOptionsClass);
  }

  public static Method getDeployVerticleMethodWithNoOptions(Class<Vertx> vertxClass) throws NoSuchMethodException {
    return vertxClass.getMethod("deployVerticle", String.class);
  }

  public static Method getVertxFactoryMethod(Class<Vertx> vertxClass) throws NoSuchMethodException {
    return vertxClass.getMethod("vertx");
  }

  public static Method getVertxCloseMethod(Class<Vertx> vertxClass) throws NoSuchMethodException {
    return vertxClass.getMethod("close");
  }
}

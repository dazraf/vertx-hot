package io.dazraf.vertx.maven;

import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import org.junit.Test;

public class VertxDynamicBindingTest {
  @Test
  public void testVertxDynamicBinding() throws ReflectiveOperationException {
    Utils.loadClassFromContextClassLoader(DeploymentOptions.class);
    Utils.getVertxFactoryMethod(Vertx.class);
    Utils.getDeploymentOptionsSetConfigMethod(DeploymentOptions.class);
    Utils.getDeployVerticleMethodWithNoOptions(Vertx.class);
    Utils.getDeployVerticleMethodWithOptions(Vertx.class, DeploymentOptions.class);
    Utils.getVertxCloseMethod(Vertx.class);
  }
}

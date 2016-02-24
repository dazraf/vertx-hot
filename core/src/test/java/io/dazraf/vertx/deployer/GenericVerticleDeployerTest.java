package io.dazraf.vertx.deployer;

import io.dazraf.vertx.HotDeployParameters;
import io.vertx.core.AsyncResult;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import org.junit.Before;
import org.junit.Test;

import java.util.Optional;

import static java.util.Arrays.asList;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;

public class GenericVerticleDeployerTest {

  private GenericVerticleDeployer deployer;
  private Vertx vertx;

  @Before
  public void setUp() {
    vertx = mock(Vertx.class);
  }

  @Test(timeout = 30_000)
  public void deploySingleVerticle() throws Throwable {
    givenVerticleReferences("foo.bar.Verticle");
    thenWillDeployVerticles("foo.bar.Verticle");
    whenDeploymentIsRunWithClasspaths("foo/bar");
  }

  @Test(timeout = 30_000)
  public void deployMultipleVerticles() throws Throwable {
    givenVerticleReferences("foo.bar.Verticle", "foo.bar.Verticle2");
    thenWillDeployVerticles("foo.bar.Verticle", "foo.bar.Verticle2");
    whenDeploymentIsRunWithClasspaths("foo/bar");
  }

  private void givenVerticleReferences(String... verticleReferences) {
    this.deployer = new GenericVerticleDeployer(
      new HotDeployParameters().withVerticleReferences(verticleReferences),
      Optional.of(vertx));
  }

  private void whenDeploymentIsRunWithClasspaths(String... classpaths) throws Throwable {
    deployer.deploy(asList(classpaths));
  }

  private void thenWillDeployVerticles(String... verticleReferences) {
    asList(verticleReferences).stream().forEach(verticle -> doAnswer(invocation -> {
      Handler<AsyncResult<String>> handler = (Handler<AsyncResult<String>>)
        invocation.getArguments()[2];
      handler.handle(success("success-id"));
      return null;
    }).when(vertx).deployVerticle(eq(verticle), any(DeploymentOptions.class), anyObject()));
  }

  private AsyncResult<String> success(final String value) {
    return new AsyncResult<String>() {
      @Override
      public String result() {
        return value;
      }

      @Override
      public Throwable cause() {
        return null;
      }

      @Override
      public boolean succeeded() {
        return true;
      }

      @Override
      public boolean failed() {
        return false;
      }
    };
  }

}
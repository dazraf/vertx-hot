package io.dazraf.vertx.buck;

import io.dazraf.vertx.HotDeploy;
import io.dazraf.vertx.HotDeploy.DeployStatus;
import io.dazraf.vertx.HotDeployParameters;
import io.vertx.core.Vertx;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static java.util.Collections.singletonList;
import static junit.framework.Assert.assertEquals;

public class BuckHotDeployTest extends DependentOnBuckBinary {

  private static final Logger LOG = LoggerFactory.getLogger(BuckHotDeployTest.class);
  private CountDownLatch latch;

  @Test
  public void simpleProject() throws Exception {
    latch = new CountDownLatch(1);
    final AtomicReference<DeployStatus> deployStatus = new AtomicReference<>();
    final AtomicInteger httpStatus = new AtomicInteger();
    final HotDeploy hotDeploy = BuckHotDeployBuilder.create()
      .withBuildTarget(":test-project")
      .withProjectRootPath("target/test-classes/project")
      .withHotDeployConfig(new HotDeployParameters()
        .withVerticleReference("io.dazraf.vertx.buck.test.App")
        .withBuildOutputDirectories(singletonList("buck-out/gen/test-project.jar"))
        .withCompileSourcePaths(singletonList("java"))
        .withShutdownCondition(() -> awaitLatch(latch))
      )
      .build();
    registerStatusListener(hotDeploy, deployStatus, httpStatus);
    hotDeploy.run();

    assertEquals(DeployStatus.DEPLOYED, deployStatus.get());
    assertEquals(200, httpStatus.get());
  }

  private void registerStatusListener(HotDeploy hotDeploy,
                                      AtomicReference<DeployStatus> deployStatusRef,
                                      AtomicInteger httpStatusRef) {
    hotDeploy.subscribeToStatusUpdates(status -> {
      final DeployStatus deployStatus = DeployStatus.valueOf(status.getString("status"));
      if (deployStatus == DeployStatus.DEPLOYED) {
        Vertx.vertx().createHttpClient().getNow(8080, "localhost", "/", response -> {
          deployStatusRef.set(deployStatus);
          httpStatusRef.set(response.statusCode());
          latch.countDown();
        });
      } else if (deployStatus == DeployStatus.FAILED) {
        deployStatusRef.set(deployStatus);
        latch.countDown();
      }
    });
  }

  private void awaitLatch(CountDownLatch latch) {
    try {
      latch.await();
    } catch (InterruptedException e) {
      LOG.error("interrupted!", e);
    }
  }

}

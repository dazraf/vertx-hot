package io.dazraf.vertx.maven;

import io.dazraf.vertx.HotDeploy;
import io.dazraf.vertx.HotDeploy.DeployStatus;
import io.dazraf.vertx.HotDeployParameters;
import io.vertx.core.Vertx;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static java.util.Collections.singletonList;
import static junit.framework.Assert.assertEquals;

public class HotDeployTest {
  private static final Logger LOGGER = LoggerFactory.getLogger(HotDeployTest.class);
  private static final String SIMPLE_TEST_PROJECT_ROOT = "src/test/testprojects/simple";
  private static final String SERVICE_TEST_PROJECT_ROOT = "src/test/testprojects/servicetest";

  @Test
  public void testFQNDeployWithoutConfig() throws Exception {
    int port = 8080; // the default port assuming config hasn't loaded
    HotDeployParameters parameters = createHotDeployParameters(SIMPLE_TEST_PROJECT_ROOT)
      .withVerticleReference("App")
      .withLiveHttpReload(false);
    hotDeployAndCheckService(port, new File(SIMPLE_TEST_PROJECT_ROOT + "/pom.xml"), parameters);
  }

  @Test
  public void testFQNDeployWithConfig() throws Exception {
    int port = 8888; // if the config loaded correctly, the http server will be found here
    HotDeployParameters parameters = createHotDeployParameters(SIMPLE_TEST_PROJECT_ROOT)
      .withVerticleReference("App")
      .withConfigFile("config.json")
      .withLiveHttpReload(false);
    hotDeployAndCheckService(port, new File(SIMPLE_TEST_PROJECT_ROOT + "/pom.xml"), parameters);
  }

  @Test
  public void testServiceWithoutConfig() throws Exception {
    int port = 8080; // the default port assuming config hasn't loaded
    HotDeployParameters parameters = createHotDeployParameters(SERVICE_TEST_PROJECT_ROOT)
      .withVerticleReference("service:simpleservice.noconfig")
      .withLiveHttpReload(false);
    hotDeployAndCheckService(port, new File(SERVICE_TEST_PROJECT_ROOT + "/pom.xml"), parameters);
  }


  @Test
  public void testServiceWithConfig() throws Exception {
    int port = 8888; // if the config loaded correctly, the service HTTP server will be here
    HotDeployParameters parameters = createHotDeployParameters(SERVICE_TEST_PROJECT_ROOT)
      .withVerticleReference("service:simpleservice")
      .withLiveHttpReload(false);
    hotDeployAndCheckService(port, new File(SERVICE_TEST_PROJECT_ROOT + "/pom.xml"), parameters);
  }


  private void hotDeployAndCheckService(int port, File buildFile,
                                        HotDeployParameters parameters) throws Exception {
    final CountDownLatch latch = new CountDownLatch(1);
    final AtomicReference<DeployStatus> deployStatusRef = new AtomicReference<>();
    final AtomicInteger httpStatusRef = new AtomicInteger();

    Runnable hotDeploy = createHotDeploy(port, buildFile, parameters, latch, deployStatusRef, httpStatusRef);

    hotDeploy.run();
    latch.await(30, TimeUnit.SECONDS);

    assertEquals(200, httpStatusRef.get());
    assertEquals(DeployStatus.DEPLOYED, deployStatusRef.get());
  }

  private Runnable createHotDeploy(int port, File buildFile, HotDeployParameters parameters,
                                   CountDownLatch latch, AtomicReference<DeployStatus> deployStatusRef,
                                   AtomicInteger httpStatusRef) throws Exception {
    HotDeploy hotDeploy = MavenHotDeployBuilder.create()
      .withBuildFile(buildFile)
      .withHotDeployParameters(parameters
        .withShutdownCondition(() -> awaitLatch(latch)))
      .build();
    hotDeploy.subscribeToStatusUpdates(status -> {
        try {
          LOGGER.info(status.encodePrettily());
          final DeployStatus deployStatus = DeployStatus.valueOf(status.getString("status"));
          if (deployStatus == DeployStatus.DEPLOYED) {
            Vertx.vertx().createHttpClient().getNow(port, "localhost", "/", response -> {
              deployStatusRef.set(deployStatus);
              httpStatusRef.set(response.statusCode());
              latch.countDown();
            });
          } else if (deployStatus == DeployStatus.FAILED) {
            deployStatusRef.set(deployStatus);
            latch.countDown();
          }
        } catch (Throwable incase) {
          LOGGER.error("ignore", incase);
        }
      });

    return () -> {
      Executors.newSingleThreadExecutor().execute(() -> {
        try {
          hotDeploy.run();
        } catch (Exception e) {
          LOGGER.error("failed to execute hotDeploy.run", e);
          latch.countDown();
        }
      });
    };
  }

  private HotDeployParameters createHotDeployParameters(String projectRootPath) throws IOException {
    return new HotDeployParameters()
      .withBuildOutputDirectories(singletonList(new File(projectRootPath).toPath().resolve("target/classes").toFile().getCanonicalPath()))
      .withResourcePaths(singletonList(new File(projectRootPath + "/src/main/resources").getAbsolutePath()));
  }

  private void awaitLatch(CountDownLatch latch) {
    try {
      latch.await();
    } catch (InterruptedException e) {
      LOGGER.error("interrupted!", e);
    }
  }
}

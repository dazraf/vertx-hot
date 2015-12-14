package io.dazraf.vertx.maven;

import io.dazraf.vertx.maven.HotDeploy.DeployStatus;
import io.dazraf.vertx.maven.compiler.Compiler;
import io.vertx.core.Vertx;
import junit.framework.Assert;
import org.apache.maven.model.Resource;
import org.apache.maven.project.MavenProject;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static junit.framework.Assert.assertEquals;

public class HotDeployTest {
  private static final Logger LOGGER = LoggerFactory.getLogger(HotDeployTest.class);

  @Test
  public void testHotDeployWithoutConfigFile() throws Exception {
    int port = 8080; // the default port assuming config hasn't loaded
    String testProject = "src/test/testprojects/simple";
    MavenProject project = createMavenProject(testProject);

    HotDeployParameters parameters = HotDeployParameters
      .create()
      .withProject(project)
      .withVerticleReference("App")
      .withLiveHttpReload(false);

    final CountDownLatch latch = new CountDownLatch(1);
    final AtomicReference<DeployStatus> deployStatusRef = new AtomicReference<>();
    final AtomicInteger httpStatusRef = new AtomicInteger();

    HotDeploy hotDeploy = createHotDeploy(port, parameters, latch, deployStatusRef, httpStatusRef);

    hotDeploy.run();
    latch.await();

    assertEquals(200, httpStatusRef.get());
    assertEquals(DeployStatus.DEPLOYED, deployStatusRef.get());
  }

  @Test
  public void testHotDeployWithConfigFile() throws Exception {
    int port = 8888; // if the config loaded correctly, the http server will be found here
    String testProject = "src/test/testprojects/simple";
    MavenProject project = createMavenProject(testProject);

    HotDeployParameters parameters = HotDeployParameters
      .create()
      .withProject(project)
      .withVerticleReference("App")
      .withConfigFileName("config.json")
      .withLiveHttpReload(false);

    final CountDownLatch latch = new CountDownLatch(1);
    final AtomicReference<DeployStatus> deployStatusRef = new AtomicReference<>();
    final AtomicInteger httpStatusRef = new AtomicInteger();

    HotDeploy hotDeploy = createHotDeploy(port, parameters, latch, deployStatusRef, httpStatusRef);

    hotDeploy.run();
    latch.await();

    assertEquals(200, httpStatusRef.get());
    assertEquals(DeployStatus.DEPLOYED, deployStatusRef.get());
  }

  @Test
  public void testService() throws Exception {
    int port = 8888; // if the config loaded correctly, the service HTTP server will be here
    String testProject = "src/test/testprojects/servicetest";
    MavenProject project = createMavenProject(testProject);

    HotDeployParameters parameters = HotDeployParameters
      .create()
      .withProject(project)
      .withVerticleReference("service:simpleservice")
      .withLiveHttpReload(false);

    final CountDownLatch latch = new CountDownLatch(1);
    final AtomicReference<DeployStatus> deployStatusRef = new AtomicReference<>();
    final AtomicInteger httpStatusRef = new AtomicInteger();

    HotDeploy hotDeploy = createHotDeploy(port, parameters, latch, deployStatusRef, httpStatusRef);

    hotDeploy.run();
    latch.await();

    assertEquals(200, httpStatusRef.get());
    assertEquals(DeployStatus.DEPLOYED, deployStatusRef.get());
  }

  private HotDeploy createHotDeploy(int port, HotDeployParameters parameters, CountDownLatch latch, AtomicReference<DeployStatus> deployStatusRef, AtomicInteger httpStatusRef) {
    return new HotDeploy(parameters, () -> awaitLatch(latch), status -> {
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
  }

  private MavenProject createMavenProject(String projectRootPath) throws IOException {
    File projectFile = new File(projectRootPath + "/pom.xml").getAbsoluteFile();
    MavenProject project = new MavenProject();
    project.setFile(projectFile);
    project.getBuild().setOutputDirectory(projectFile.getParentFile().toPath().resolve("target/classes").toFile().getCanonicalPath());
    Resource resource = new Resource();
    resource.setDirectory(new File(projectRootPath + "/src/main/resources").getAbsolutePath());
    project.getResources().add(resource);
    return project;
  }

  private void awaitLatch(CountDownLatch latch) {
    try {
      latch.await();
    } catch (InterruptedException e) {
      LOGGER.error("interrupted!", e);
    }
  }
}

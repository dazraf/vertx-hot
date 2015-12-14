package io.dazraf.vertx.maven;

import io.dazraf.vertx.maven.compiler.Compiler;
import junit.framework.Assert;
import org.apache.maven.model.Resource;
import org.apache.maven.project.MavenProject;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

import static junit.framework.Assert.assertEquals;

public class HotDeployTest {
  private static final Logger LOGGER = LoggerFactory.getLogger(HotDeployTest.class);

  @Test
  public void testHotDeployWithoutLogFile() throws Exception {
    String testProject = "src/test/testproject/testproject1";
    MavenProject project = createMavenProject(testProject);

    HotDeployParameters parameters = HotDeployParameters
      .create()
      .withProject(project)
      .withVerticleReference("App")
      .withLiveHttpReload(true);

    final CountDownLatch latch = new CountDownLatch(1);
    final AtomicReference<HotDeploy.DeployStatus> deployStatusReference = new AtomicReference<>();

    HotDeploy hotDeploy = new HotDeploy(parameters, () -> awaitLatch(latch), status -> {
      try {
        LOGGER.info(status.encodePrettily());
        final HotDeploy.DeployStatus deployStatus = HotDeploy.DeployStatus.valueOf(status.getString("status"));
        if (deployStatus.equals(HotDeploy.DeployStatus.DEPLOYED) ||
          deployStatus.equals(HotDeploy.DeployStatus.FAILED)) {
          deployStatusReference.set(deployStatus);
          latch.countDown();
        }
      } catch (Throwable incase) {
        LOGGER.error("ignore", incase);
      }
    });
    hotDeploy.run();
    latch.await();
    assertEquals(HotDeploy.DeployStatus.DEPLOYED, deployStatusReference.get());
  }

  private MavenProject createMavenProject(String projectRootPath) throws IOException {
    File projectFile = new File(projectRootPath + "/pom.xml").getAbsoluteFile();
    MavenProject project = new MavenProject();
    project.setFile(projectFile);
    project.getBuild().setOutputDirectory(projectFile.getParentFile().toPath().resolve("target/classes").toFile().getCanonicalPath());
    Resource resource = new Resource();
    resource.setDirectory(new File(projectRootPath + "/resources").getAbsolutePath());
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

package io.dazraf.vertx.buck.paths;

import io.dazraf.vertx.HotDeployParameters;
import org.junit.Test;

import java.util.*;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static java.util.Optional.of;
import static org.junit.Assert.*;

public class BuckPathResolverTest {

  private static final String PROJECT_ROOT = "/root/path";
  private static final String BUILD_TARGET_NAME = "app-bin";
  private static final String VERTICLE = "foo.bar.Foobar";

  private BuckPathResolver resolver;

  @Test
  public void defaultConfigurationAssumesGeneratedJar() throws Exception {
    givenStandardConfiguration();
    expectTheClasspathToBe(
      String.format("%s/buck-out/gen/%s.jar", PROJECT_ROOT, BUILD_TARGET_NAME)
    );
  }

  @Test
  public void customBuildDirectoryConfigurationMakesNoAssumptions() throws Exception {
    givenConfigurationWithBuildOutputDirectories("foo/bar.jar");
    expectTheClasspathToBe(
      String.format("%s/foo/bar.jar", PROJECT_ROOT)
    );
  }

  private void givenStandardConfiguration() {
    resolver = new BuckPathResolver(new HotDeployParameters()
      .withVerticleReference(VERTICLE)
      .withCompileSourcePaths(singletonList("src")),
      of(PROJECT_ROOT),
      "//:" + BUILD_TARGET_NAME
    );
  }

  private void givenConfigurationWithBuildOutputDirectories(String... buildOutputDirs) {
    resolver = new BuckPathResolver(new HotDeployParameters()
      .withVerticleReference(VERTICLE)
      .withCompileSourcePaths(singletonList("src"))
      .withBuildOutputDirectories(asList(buildOutputDirs)),
      of(PROJECT_ROOT),
      "//:" + BUILD_TARGET_NAME
    );
  }

  private void expectTheClasspathToBe(String... expectedClasspath) {
    final List<String> actual = resolver.getClasspath();
    assertEquals(asList(expectedClasspath), actual);
  }

}
package io.dazraf.vertx.maven.paths;

import io.dazraf.vertx.paths.ExtraPath;
import io.dazraf.vertx.HotDeployParameters;
import org.junit.Test;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static io.dazraf.vertx.paths.ExtraPath.VertxHotAction.*;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class MavenPathResolverTest {
  private static final String PROJECT_ROOT = "/myproject";
  private static final String RELOAD_BROWSER_EXTRA_PATH = "reloadBrowserExtraPath";
  private static final String REDEPLOY_EXTRA_PATH = "redeployExtraPath";
  private static final String COMPILE_EXTRA_PATH = "compileExtraPath";
  private static final String CONFIG_PATH = "config1.json";
  private static final String CONFIG_PATH2 = "config2.json";
  private static final String POM_XML_PATH = "pom.xml";
  private static final String RESOURCES_PATH = "src/test/resources";
  private static final String RESOURCES_PATH2 = "src/test/resources2";

  @Test
  public void thatFileClassificationsWork() {
    Path root = Paths.get(PROJECT_ROOT);
    Path resourcesFullPath = root.resolve(RESOURCES_PATH);
    final Path pomFullPath = root.resolve(POM_XML_PATH);

    HotDeployParameters hotDeployParameters = new HotDeployParameters()
      .withLiveHttpReload(true)
      .withConfigFile(CONFIG_PATH)
      .withResourcePaths(singletonList(resourcesFullPath.toString()))
      .withBuildResources(true)
      .withExtraPaths(asList(
        new ExtraPath().withPath(RELOAD_BROWSER_EXTRA_PATH).withAction(Refresh),
        new ExtraPath().withPath(REDEPLOY_EXTRA_PATH).withAction(Redeploy),
        new ExtraPath().withPath(COMPILE_EXTRA_PATH).withAction(Recompile)
      ));

    MavenPathResolver pathResolver = new MavenPathResolver(hotDeployParameters, pomFullPath.toFile());
    List<Path> compilePaths = pathResolver.pathsThatRequireCompile();
    assertEquals(toSet(root, POM_XML_PATH, RESOURCES_PATH, COMPILE_EXTRA_PATH), pathsToStringSet(compilePaths));

    List<Path> deployPaths = pathResolver.pathsThatRequireRedeploy();
    Set<String> expected = Stream.concat(toSet(resourcesFullPath, CONFIG_PATH).stream(), toSet(root, REDEPLOY_EXTRA_PATH).stream()).collect(Collectors.toSet());
    assertEquals(expected, pathsToStringSet(deployPaths));

    List<Path> refreshPaths = pathResolver.pathsThatRequireBrowserRefresh();
    assertEquals(toSet(root, RELOAD_BROWSER_EXTRA_PATH), pathsToStringSet(refreshPaths));
  }

  @Test
  public void thatConfigFileIsCorrectlyLocated() {
    Path root = Paths.get(PROJECT_ROOT);
    Path resourcesFullPath = root.resolve(RESOURCES_PATH2);
    final Path pomFullPath = root.resolve(POM_XML_PATH);

    HotDeployParameters hotDeployParameters = new HotDeployParameters()
      .withLiveHttpReload(true)
      .withConfigFile(CONFIG_PATH2)
      .withBuildResources(true)
      .withResourcePaths(singletonList(resourcesFullPath.toString()))
      .withExtraPaths(asList(
        new ExtraPath().withPath(RELOAD_BROWSER_EXTRA_PATH).withAction(Refresh),
        new ExtraPath().withPath(REDEPLOY_EXTRA_PATH).withAction(Redeploy),
        new ExtraPath().withPath(COMPILE_EXTRA_PATH).withAction(Recompile)
      ));

    MavenPathResolver pathResolver = new MavenPathResolver(hotDeployParameters, pomFullPath.toFile());
    final List<Path> paths = pathResolver.pathsThatRequireRedeploy();
    assertTrue(paths.contains(resourcesFullPath.resolve(CONFIG_PATH2)));
  }

  private Set<String> toSet(Path root, String... args) {
    return Arrays.stream(args).map(p -> root.resolve(p).toString()).collect(Collectors.toSet());
  }

  private Set<String> pathsToStringSet(Collection<Path> paths) {
    return paths.stream().map(Path::toString).collect(Collectors.toSet());
  }
}

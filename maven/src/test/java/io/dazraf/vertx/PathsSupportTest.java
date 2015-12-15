package io.dazraf.vertx;

import io.dazraf.vertx.maven.MavenHotDeployParameters;
import org.apache.maven.model.Resource;
import org.apache.maven.project.MavenProject;
import org.junit.Test;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static io.dazraf.vertx.ExtraPath.VertxHotAction.*;
import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class PathsSupportTest {
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
    Resource resource = mock(Resource.class);
    Path resourcesFullPath = root.resolve(RESOURCES_PATH);
    when(resource.getDirectory()).thenReturn(resourcesFullPath.toString());

    MavenProject project = mock(MavenProject.class);
    final Path pomFullPath = root.resolve(POM_XML_PATH);

    when(project.getFile()).thenReturn(pomFullPath.toFile());
    when(project.getResources()).thenReturn(Collections.singletonList(resource));

    HotDeployParameters hotDeployParameters = new MavenHotDeployParameters()
      .withProject(project)
      .withLiveHttpReload(true)
      .withConfigFileName(CONFIG_PATH)
      .withBuildResources(true)
      .withExtraPaths(asList(
        new ExtraPath().withPath(RELOAD_BROWSER_EXTRA_PATH).withAction(Refresh),
        new ExtraPath().withPath(REDEPLOY_EXTRA_PATH).withAction(Redeploy),
        new ExtraPath().withPath(COMPILE_EXTRA_PATH).withAction(Recompile)
      ));

    PathsSupport pathsSupport = new PathsSupport(hotDeployParameters);
    List<Path> compilePaths = pathsSupport.pathsThatRequireCompile();
    assertEquals(toSet(root, POM_XML_PATH, RESOURCES_PATH, COMPILE_EXTRA_PATH), pathsToStringSet(compilePaths));

    List<Path> deployPaths = pathsSupport.pathsThatRequireRedeploy();
    Set<String> expected = Stream.concat(toSet(resourcesFullPath, CONFIG_PATH).stream(), toSet(root, REDEPLOY_EXTRA_PATH).stream()).collect(Collectors.toSet());
    assertEquals(expected, pathsToStringSet(deployPaths));

    List<Path> refreshPaths = pathsSupport.pathsThatRequireBrowserRefresh();
    assertEquals(toSet(root, RELOAD_BROWSER_EXTRA_PATH), pathsToStringSet(refreshPaths));
  }


  @Test
  public void thatConfigFileIsCorrectlyLocated() {
    Path root = Paths.get(PROJECT_ROOT);
    Resource resource = mock(Resource.class);
    Path resourcesFullPath = root.resolve(RESOURCES_PATH2);
    when(resource.getDirectory()).thenReturn(resourcesFullPath.toString());

    MavenProject project = mock(MavenProject.class);
    final Path pomFullPath = root.resolve(POM_XML_PATH);

    when(project.getFile()).thenReturn(pomFullPath.toFile());
    when(project.getResources()).thenReturn(Collections.singletonList(resource));

    HotDeployParameters hotDeployParameters = new MavenHotDeployParameters()
      .withProject(project)
      .withLiveHttpReload(true)
      .withConfigFileName(CONFIG_PATH2)
      .withBuildResources(true)
      .withExtraPaths(asList(
        new ExtraPath().withPath(RELOAD_BROWSER_EXTRA_PATH).withAction(Refresh),
        new ExtraPath().withPath(REDEPLOY_EXTRA_PATH).withAction(Redeploy),
        new ExtraPath().withPath(COMPILE_EXTRA_PATH).withAction(Recompile)
      ));

    PathsSupport pathSupport = new PathsSupport(hotDeployParameters);
    final List<Path> paths = pathSupport.pathsThatRequireRedeploy();
    paths.contains(resourcesFullPath.resolve(CONFIG_PATH2));
  }

  private Set<String> toSet(Path root, String... args) {
    return Arrays.stream(args).map(p -> root.resolve(p).toString()).collect(Collectors.toSet());
  }

  private Set<String> pathsToStringSet(Collection<Path> paths) {
    return paths.stream().map(Path::toString).collect(Collectors.toSet());
  }
}

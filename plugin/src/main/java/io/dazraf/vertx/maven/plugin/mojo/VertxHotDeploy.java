package io.dazraf.vertx.maven.plugin.mojo;

import io.dazraf.vertx.maven.HotDeploy;
import org.apache.maven.model.Resource;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;

import java.util.List;
import java.util.stream.Collectors;

import static java.util.Optional.ofNullable;
import static java.util.function.Function.identity;
import static java.util.stream.Stream.of;

@Mojo(name = "hot",
  requiresProject = true,
  threadSafe = true,
  requiresDependencyResolution = ResolutionScope.COMPILE_PLUS_RUNTIME,
  requiresDependencyCollection = ResolutionScope.COMPILE_PLUS_RUNTIME)
public class VertxHotDeploy extends AbstractMojo {
  @Parameter(property = "verticleClass", required = true)
  private String verticleClassName = "";

  @Parameter(property = "configFile", required = false)
  private String configFile = null;

  @Parameter(property = "liveHttpReload", required = false)
  private boolean liveHttpReload = true;

  /**
   * The enclosing project.
   */
  @Parameter(defaultValue = "${project}", readonly = true)
  private MavenProject project = null;

  @Override
  public void execute() throws MojoExecutionException, MojoFailureException {
    Log log = getLog();
    try {
      // collect all the paths to watched
      List<String> watchedPaths = of(
        project.getCompileSourceRoots().stream(), // set of compile sources
        project.getResources().stream().map(Resource::getDirectory), // the resources
        of(project.getFile().getAbsolutePath()) // the pom file itself
      )
        .flatMap(identity())
        .collect(Collectors.toList());

      HotDeploy.run(project, watchedPaths, verticleClassName, ofNullable(configFile), liveHttpReload);
    } catch (Exception e) {
      log.error(e);
      throw new MojoExecutionException("Failed to startup hot redeploy", e);
    }
  }
}

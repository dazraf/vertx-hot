package io.dazraf.vertx.maven.plugin.mojo;

import io.dazraf.vertx.HotDeploy;
import io.dazraf.vertx.HotDeployParameters;
import io.dazraf.vertx.maven.MavenHotDeployBuilder;
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

import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;

@Mojo(name = "hot",
  requiresProject = true,
  threadSafe = true,
  requiresDependencyResolution = ResolutionScope.COMPILE_PLUS_RUNTIME,
  requiresDependencyCollection = ResolutionScope.COMPILE_PLUS_RUNTIME)
public class VertxHotDeploy extends AbstractMojo {
  @Parameter(property = "verticleReference", required = true)
  private String verticleReference = "";

  @Parameter(property = "configFile", required = false)
  private String configFile = null;

  @Parameter(property = "liveHttpReload", required = false, defaultValue = "true")
  private boolean liveHttpReload = true;

  @Parameter(property = "buildResources", required = false, defaultValue = "false")
  private boolean buildResources = false;

  @Parameter(property = "notificationPort", required = false, defaultValue = "9999")
  private int notificationPort = 9999;

  @Parameter(property = "extraPaths", required = false, name = "extraPaths")
  private List<ExtraPathParam> extraPaths;

  /**
   * The enclosing project.
   */
  @Parameter(defaultValue = "${project}", readonly = true)
  private MavenProject project = null;

  @Override
  public void execute() throws MojoExecutionException, MojoFailureException {
    Log log = getLog();
    try {
      MavenHotDeployBuilder.create()
              .withBuildFile(project.getFile())
              .withHotDeployParameters(new HotDeployParameters()
                      .withVerticleReference(verticleReference)
                      .withConfigFile(configFile)
                      .withLiveHttpReload(liveHttpReload)
                      .withBuildResources(buildResources)
                      .withNotificationPort(notificationPort)
                      .withResourcePaths(project.getResources().stream()
                              .map(Resource::getDirectory).collect(toList()))
                      .withCompileSourcePaths(project.getCompileSourceRoots())
                      .withBuildOutputDirectories(singletonList(project.getBuild()
                              .getOutputDirectory()))
                      .withExtraPaths(extraPaths != null ? extraPaths.stream()
                              .map(ExtraPathParam::getExtraPath).collect(toList()) : null))
              .build()
              .run();
    } catch (Exception e) {
      log.error(e);
      throw new MojoExecutionException("Failed to startup hot redeploy", e);
    }
  }
}

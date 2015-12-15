package io.dazraf.vertx.maven.plugin.mojo;

import io.dazraf.vertx.HotDeploy;
import io.dazraf.vertx.maven.MavenHotDeployParameters;
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
      HotDeploy.run(MavenHotDeployParameters.create()
        .withProject(project)
        .withVerticleReference(verticleReference)
        .withConfigFileName(configFile)
        .withLiveHttpReload(liveHttpReload)
        .withBuildResources(buildResources)
        .withNotificationPort(notificationPort)
        .withExtraPaths(extraPaths != null ?
                extraPaths.stream().map(ExtraPathParam::getExtraPath).collect(Collectors.toList()) : null));
    } catch (Exception e) {
      log.error(e);
      throw new MojoExecutionException("Failed to startup hot redeploy", e);
    }
  }
}

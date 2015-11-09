package io.dazraf.vertx.maven.plugin.mojo;

import io.dazraf.vertx.maven.HotDeploy;
import io.dazraf.vertx.maven.HotDeployParameters;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;

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

  @Parameter(property = "liveHttpReload", required = false, defaultValue = "true")
  private boolean liveHttpReload = true;

  @Parameter(property = "buildResources", required = false, defaultValue = "false")
  private boolean buildResources = false;

  @Parameter(property = "notificationPort", required = false, defaultValue = "9999")
  private int notificationPort = 9999;

  /**
   * The enclosing project.
   */
  @Parameter(defaultValue = "${project}", readonly = true)
  private MavenProject project = null;

  @Override
  public void execute() throws MojoExecutionException, MojoFailureException {
    Log log = getLog();
    try {
      HotDeploy.run(HotDeployParameters.create()
        .withProject(project)
        .withVerticleClassName(verticleClassName)
        .withConfigFileName(configFile)
        .withLiveHttpReload(liveHttpReload)
        .withBuildResources(buildResources)
        .withNotificationPort(notificationPort));
    } catch (Exception e) {
      log.error(e);
      throw new MojoExecutionException("Failed to startup hot redeploy", e);
    }
  }
}

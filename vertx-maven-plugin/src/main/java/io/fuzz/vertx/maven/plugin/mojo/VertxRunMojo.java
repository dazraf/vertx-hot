package io.fuzz.vertx.maven.plugin.mojo;

import io.fuzz.vertx.maven.HotDeploy;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;

@Mojo(name="hot!", requiresProject = true, threadSafe = false, requiresDependencyResolution = ResolutionScope.COMPILE_PLUS_RUNTIME)

public class VertxRunMojo extends AbstractMojo {
  @Parameter(property="verticleClass")
  private String verticleClassName = "";

  @Override
  public void execute() throws MojoExecutionException, MojoFailureException {
    getLog().info("Here I am Yeah!");
    try {
      HotDeploy.run(verticleClassName);
    } catch (Exception e) {
      throw new MojoExecutionException("failed to startup hot redeploy", e);
    }
  }
}

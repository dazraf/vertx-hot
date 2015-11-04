package io.dazraf.vertx.maven.plugin.mojo;

import io.dazraf.vertx.maven.HotDeploy;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.model.Resource;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toList;

@Mojo(name = "hot",
  requiresProject = true,
  threadSafe = true,
  requiresDependencyResolution = ResolutionScope.COMPILE_PLUS_RUNTIME,
  requiresDependencyCollection = ResolutionScope.COMPILE_PLUS_RUNTIME)
public class VertxHotDeploy extends AbstractMojo {
  @Parameter(property = "verticleClass")
  private String verticleClassName = "";

  @Parameter(property = "configFile")
  private String configFile = null;

  @Parameter(property = "liveHttpReload")
  private boolean liveHttpReload = false;

  /**
   * The enclosing project.
   */
  @Parameter(defaultValue = "${project}", readonly = true)
  protected MavenProject project;

  @Override
  public void execute() throws MojoExecutionException, MojoFailureException {
    Log log = getLog();
    try {
      List<String> classPath = computeClasspath();
      List<String> watchedPaths = Stream.concat(
        project.getCompileSourceRoots().stream(),
        project.getResources().stream().map(Resource::getDirectory)
      ).collect(Collectors.toList());
      File pomFile = project.getFile();
      HotDeploy.run(pomFile, verticleClassName, classPath, ofNullable(configFile), watchedPaths, liveHttpReload);
    } catch (Exception e) {
      log.error(e);
      throw new MojoExecutionException("Failed to startup hot redeploy", e);
    }
  }

  /**
   * Compute the classpath from the specified Classpath. The computed classpath is based on the classpathScope. The
   * plugin cannot know from maven the phase it is executed in. So we have to depend on the user to tell us he wants
   * the scope in which the plugin is expected to be executed.
   *
   * @return a list of class path elements
   */
  private List<String> computeClasspath() {
    List<Artifact> artifacts = new ArrayList<>();
    List<File> theClasspathFiles = new ArrayList<>();
    List<String> resultList = new ArrayList<>();

    collectProjectArtifactsAndClasspath(artifacts, theClasspathFiles);

    resultList.addAll(
      theClasspathFiles.stream()
        .map(File::getAbsolutePath)
        .collect(toList()));

    for (Artifact artifact : artifacts) {
      getLog().debug("dealing with " + artifact);
      resultList.add(artifact.getFile().getAbsolutePath());
    }

    return resultList;
  }

  /**
   * Collects the project artifacts in the specified List and the project specific classpath (build output and build
   * test output) Files in the specified List, depending on the plugin classpathScope value.
   *
   * @param artifacts         the list where to collect the scope specific artifacts
   * @param theClasspathFiles the list where to collect the scope specific output directories
   */
  @SuppressWarnings("deprecation")
  protected void collectProjectArtifactsAndClasspath(List<Artifact> artifacts, List<File> theClasspathFiles) {
    artifacts.addAll(project.getCompileArtifacts());
    artifacts.addAll(project.getRuntimeArtifacts());
    artifacts.addAll(project.getSystemArtifacts());
    theClasspathFiles.add(new File(project.getBuild().getOutputDirectory()));
    getLog().debug("Collected project artifacts " + artifacts);
    getLog().debug("Collected project classpath " + theClasspathFiles);
  }

}

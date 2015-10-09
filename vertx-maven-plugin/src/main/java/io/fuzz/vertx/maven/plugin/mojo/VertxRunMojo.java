package io.fuzz.vertx.maven.plugin.mojo;

import com.google.common.collect.Collections2;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import io.fuzz.vertx.maven.HotDeploy;
import org.apache.commons.collections4.MapUtils;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Stream.of;

@Mojo(name = "run",
  requiresProject = true,
  threadSafe = true,
  requiresDependencyResolution = ResolutionScope.COMPILE_PLUS_RUNTIME,
  requiresDependencyCollection = ResolutionScope.COMPILE_PLUS_RUNTIME)
public class VertxRunMojo extends AbstractMojo {
  @Parameter(property = "verticleClass")
  private String verticleClassName = "";

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
      HotDeploy.run(verticleClassName, classPath);
    } catch (Exception e) {
      log.error(e);
      throw new MojoExecutionException("failed to startup hot redeploy", e);
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
  protected void collectProjectArtifactsAndClasspath(List<Artifact> artifacts, List<File> theClasspathFiles) {
    artifacts.addAll(project.getCompileArtifacts());
    artifacts.addAll(project.getRuntimeArtifacts());
    artifacts.addAll(project.getSystemArtifacts());
    theClasspathFiles.add(new File(project.getBuild().getOutputDirectory()));
    getLog().debug("Collected project artifacts " + artifacts);
    getLog().debug("Collected project classpath " + theClasspathFiles);
  }

  private void dumpClasspath(String contextName, ClassLoader classLoader) {
    if (classLoader instanceof URLClassLoader) {
      URLClassLoader urlClassLoader = (URLClassLoader)classLoader;
      System.out.println("ClassLoader '" + contextName + "' paths");
      of(urlClassLoader.getURLs()).forEach(System.out::println);
    } else {
      getLog().info("classloader is not a URL classloader");
    }
  }

}

package io.dazraf.vertx.maven;

import io.dazraf.vertx.ExtraPath;
import io.dazraf.vertx.HotDeployParameters;
import io.dazraf.vertx.compiler.Compiler;
import io.dazraf.vertx.maven.compiler.MavenCompiler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.apache.maven.model.Resource;
import org.apache.maven.project.MavenProject;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;

public class MavenHotDeployParameters implements HotDeployParameters {

  public static MavenHotDeployParameters create() {
    return new MavenHotDeployParameters();
  }

  private MavenProject project;
  private String verticleReference;
  private Optional<String> configFileName = Optional.empty();
  private boolean liveHttpReload;
  private boolean buildResources;
  private int notificationPort;

  private Optional<List<ExtraPath>> extraPaths = Optional.empty();

  public MavenHotDeployParameters withProject(MavenProject project) {
    this.project = project;
    return this;
  }

  public MavenHotDeployParameters withVerticleReference(String verticleReference) {
    this.verticleReference = verticleReference;
    return this;
  }

  public MavenHotDeployParameters withConfigFileName(String configFileName) {
    this.configFileName = Optional.ofNullable(configFileName);
    return this;
  }

  public MavenHotDeployParameters withLiveHttpReload(boolean liveHttpReload) {
    this.liveHttpReload = liveHttpReload;
    return this;
  }

  public MavenHotDeployParameters withBuildResources(boolean buildResources) {
    this.buildResources = buildResources;
    return this;
  }

  public MavenHotDeployParameters withNotificationPort(int notificationPort) {
    this.notificationPort = notificationPort;
    return this;
  }

  public MavenHotDeployParameters withExtraPaths(List<ExtraPath> extraPaths) {
    this.extraPaths = Optional.ofNullable(extraPaths);
    return this;
  }

  public MavenProject getProject() {
    return project;
  }

  @Override
  public String getVerticleReference() {
    return verticleReference;
  }

  @Override
  public Optional<String> getConfigFileName() {
    return configFileName;
  }

  @Override
  public boolean isLiveHttpReload() {
    return liveHttpReload;
  }

  @Override
  public boolean isBuildResources() {
    return buildResources;
  }

  @Override
  public int getNotificationPort() {
    return notificationPort;
  }

  @Override
  public Optional<List<ExtraPath>> getExtraPaths() {
    return extraPaths;
  }

  @Override
  public List<String> getClasspath() {
    List<String> classPath = new ArrayList<>();
    classPath.addAll(getResourcePaths());
    classPath.addAll(getBuildOutputDirectories());
    return classPath;
  }

  @Override
  public List<String> getBuildOutputDirectories() {
    return singletonList(project.getBuild().getOutputDirectory());
  }

  @Override
  public Class<? extends Compiler> getCompilerClass() {
    return MavenCompiler.class;
  }

  @Override
  public List<String> getResourcePaths() {
    List<String> resources = new ArrayList<>();
    // precedence to load from the resources folders rather than the build
    project.getResources().stream().map(Resource::getDirectory).forEach(resources::add);
    return resources;
  }

  @Override
  public List<String> getCompileSourcePaths() {
    return project.getCompileSourceRoots();
  }

  @Override
  public File getBuildFile() {
    return project.getFile();
  }

  @Override
  public String toString() {
    JsonObject result = new JsonObject();
    result.put("verticleReference", verticleReference)
            .put("configFileName", configFileName.orElse("undefined"))
            .put("liveHttpReload", liveHttpReload)
            .put("buildResources", buildResources)
            .put("pom", project != null ? project.getFile().getName() : "undefined")
            .put("extraPaths", new JsonArray(
                    extraPaths
                            .map(Arrays::asList)
                            .orElse(emptyList())
            ));
    return result.toString();
  }
}

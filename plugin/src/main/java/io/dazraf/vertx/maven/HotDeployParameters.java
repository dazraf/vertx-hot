package io.dazraf.vertx.maven;

import io.dazraf.vertx.maven.plugin.mojo.ExtraPath;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.apache.maven.project.MavenProject;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static java.util.Collections.emptyList;

public class HotDeployParameters {
  private MavenProject project;
  private String verticleReference;
  private Optional<String> configFileName = Optional.empty();
  private boolean liveHttpReload;
  private boolean buildResources;
  private int notificationPort;
  private Optional<List<ExtraPath>> extraPaths = Optional.empty();

  public static HotDeployParameters create() {
    return new HotDeployParameters();
  }

  public HotDeployParameters withProject(MavenProject project) {
    this.project = project;
    return this;
  }

  public HotDeployParameters withVerticleReference(String verticleReference) {
    this.verticleReference = verticleReference;
    return this;
  }

  public HotDeployParameters withConfigFileName(String configFileName) {
    this.configFileName = Optional.ofNullable(configFileName);
    return this;
  }

  public HotDeployParameters withLiveHttpReload(boolean liveHttpReload) {
    this.liveHttpReload = liveHttpReload;
    return this;
  }

  public HotDeployParameters withBuildResources(boolean buildResources) {
    this.buildResources = buildResources;
    return this;
  }

  public HotDeployParameters withNotificationPort(int notificationPort) {
    this.notificationPort = notificationPort;
    return this;
  }

  public HotDeployParameters withExtraPaths(List<ExtraPath> extraPaths) {
    this.extraPaths = Optional.ofNullable(extraPaths);
    return this;
  }

  public MavenProject getProject() {
    return project;
  }

  public String getVerticleReference() {
    return verticleReference;
  }

  public Optional<String> getConfigFileName() {
    return configFileName;
  }

  public boolean isLiveHttpReload() {
    return liveHttpReload;
  }

  public boolean isBuildResources() {
    return buildResources;
  }

  public int getNotificationPort() {
    return notificationPort;
  }

  public Optional<List<ExtraPath>> getExtraPaths() {
    return extraPaths;
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

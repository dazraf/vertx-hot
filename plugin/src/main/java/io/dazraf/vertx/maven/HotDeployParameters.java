package io.dazraf.vertx.maven;

import io.vertx.core.json.JsonObject;
import org.apache.maven.project.MavenProject;

import java.util.Optional;

public class HotDeployParameters {
  private MavenProject project;
  private String verticleClassName;
  private Optional<String> configFileName;
  private boolean liveHttpReload;
  private boolean buildResources;
  private int notificationPort;

  public static HotDeployParameters create() {
    return new HotDeployParameters();
  }

  public HotDeployParameters withProject(MavenProject project) {
    this.project = project;
    return this;
  }

  public HotDeployParameters withVerticleClassName(String verticleClassName) {
    this.verticleClassName = verticleClassName;
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

  public MavenProject getProject() {
    return project;
  }

  public String getVerticleClassName() {
    return verticleClassName;
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

  @Override
  public String toString() {
    JsonObject result = new JsonObject();
    result.put("verticleClassName", verticleClassName)
      .put("configFileName", configFileName.orElse("undefined"))
      .put("liveHttpReload", liveHttpReload)
      .put("buildResources", buildResources)
      .put("pom", project != null ? project.getFile().getName() : "undefined");
    return result.toString();
  }
}

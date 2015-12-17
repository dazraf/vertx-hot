package io.dazraf.vertx;

import io.dazraf.vertx.paths.ExtraPath;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static java.util.Collections.emptyList;

public class HotDeployParameters {

  private String verticleReference;
  private Optional<String> configFilePath;
  private boolean liveHttpReload;
  private boolean buildResources;
  private int notificationPort;
  private List<String> compileSourcePaths = new ArrayList<>();
  private List<String> resourcePaths = new ArrayList<>();
  private List<String> buildOutputDirectories = new ArrayList<>();
  private Optional<List<ExtraPath>> extraPaths = Optional.empty();

  public HotDeployParameters withVerticleReference(String verticleReference) {
    this.verticleReference = verticleReference;
    return this;
  }

  public HotDeployParameters withConfigFile(String configFilePath) {
    this.configFilePath = Optional.ofNullable(configFilePath);
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

  public HotDeployParameters withResourcePaths(List<String> resourcePaths) {
    this.resourcePaths = resourcePaths;
    return this;
  }

  public HotDeployParameters withCompileSourcePaths(List<String> compileSourcePaths) {
    this.compileSourcePaths = compileSourcePaths;
    return this;
  }

  public HotDeployParameters withBuildOutputDirectories(List<String> buildOutputDirectories) {
    this.buildOutputDirectories = buildOutputDirectories;
    return this;
  }

  public String getVerticleReference() {
    return verticleReference;
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

  public List<String> getCompileSourcePaths() {
    return compileSourcePaths;
  }

  public List<String> getResourcePaths() {
    return resourcePaths;
  }

  public List<String> getBuildOutputDirectories() {
    return buildOutputDirectories;
  }

  public Optional<String> getConfigFilePath() {
    return configFilePath;
  }

  public Optional<List<ExtraPath>> getExtraPaths() {
    return extraPaths;
  }

  @Override
  public String toString() {
    JsonObject result = new JsonObject();
    result.put("verticleReference", verticleReference)
            .put("configFilePath", configFilePath.orElse("undefined"))
            .put("liveHttpReload", liveHttpReload)
            .put("buildResources", buildResources)
            .put("notificationPort", notificationPort)
            .put("extraPaths", new JsonArray(
                    extraPaths
                            .map(Arrays::asList)
                            .orElse(emptyList())
            ))
            .put("compileSourcePaths", getCompileSourcePaths())
            .put("resourcePaths", getResourcePaths())
            .put("buildOutputDirectories", getBuildOutputDirectories());
    return result.toString();
  }
}

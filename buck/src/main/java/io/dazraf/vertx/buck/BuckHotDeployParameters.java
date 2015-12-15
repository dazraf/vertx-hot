package io.dazraf.vertx.buck;

import io.dazraf.vertx.ExtraPath;
import io.dazraf.vertx.HotDeployParameters;
import io.dazraf.vertx.buck.compiler.BuckCompiler;
import io.dazraf.vertx.compiler.Compiler;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class BuckHotDeployParameters implements HotDeployParameters {

  private List<String> compileSourcePaths;
  private List<String> resourcePaths;
  private Optional<List<ExtraPath>> extraPaths;
  private String verticleReference;
  private Optional<String> configFileName = Optional.empty();
  private boolean liveHttpReload;
  private boolean buildResources;
  private int notificationPort;
  private String buckFile;
  private List<String> buildOutputDirectories;
  private String buildTarget;

  public BuckHotDeployParameters withResourcePaths(List<String> resourcePaths) {
    this.resourcePaths = resourcePaths;
    return this;
  }

  public BuckHotDeployParameters withCompileSourcePaths(List<String> compileSourcePaths) {
    this.compileSourcePaths = compileSourcePaths;
    return this;
  }

  public BuckHotDeployParameters withBuckFile(String buckFile) {
    this.buckFile = buckFile;
    return this;
  }

  public BuckHotDeployParameters withBuildOutputDirectories(List<String> buildOutputDirectories) {
    this.buildOutputDirectories = buildOutputDirectories;
    return this;
  }

  public BuckHotDeployParameters withVerticleReference(String verticleReference) {
    this.verticleReference = verticleReference;
    return this;
  }

  public BuckHotDeployParameters withConfigFileName(String configFileName) {
    this.configFileName = Optional.ofNullable(configFileName);
    return this;
  }

  public BuckHotDeployParameters withLiveHttpReload(boolean liveHttpReload) {
    this.liveHttpReload = liveHttpReload;
    return this;
  }

  public BuckHotDeployParameters withBuildResources(boolean buildResources) {
    this.buildResources = buildResources;
    return this;
  }

  public BuckHotDeployParameters withNotificationPort(int notificationPort) {
    this.notificationPort = notificationPort;
    return this;
  }

  public BuckHotDeployParameters withExtraPaths(List<ExtraPath> extraPaths) {
    this.extraPaths = Optional.ofNullable(extraPaths);
    return this;
  }

  public BuckHotDeployParameters withBuildTarget(String buildTarget) {
    this.buildTarget = buildTarget;
    return this;
  }

  @Override
  public List<String> getCompileSourcePaths() {
    return compileSourcePaths;
  }

  @Override
  public List<String> getResourcePaths() {
    return resourcePaths;
  }

  @Override
  public boolean isBuildResources() {
    return buildResources;
  }

  @Override
  public boolean isLiveHttpReload() {
    return liveHttpReload;
  }

  @Override
  public int getNotificationPort() {
    return notificationPort;
  }

  @Override
  public String getVerticleReference() {
    return verticleReference;
  }

  @Override
  public File getBuildFile() {
    return new File(buckFile);
  }

  @Override
  public Optional<String> getConfigFileName() {
    return configFileName;
  }

  @Override
  public Optional<List<ExtraPath>> getExtraPaths() {
    return extraPaths;
  }

  @Override
  public List<String> getBuildOutputDirectories() {
    return buildOutputDirectories;
  }

  @Override
  public Class<? extends Compiler> getCompilerClass() {
    return BuckCompiler.class;
  }

  public String getBuildTarget() {
    return buildTarget;
  }

  @Override
  public List<String> getClasspath() {
    List<String> classpath = new ArrayList<>();
    classpath.addAll(resourcePaths);
    classpath.addAll(getBuildOutputDirectories());
    return classpath;
  }

  @Override
  public String toString() {
    return "BuckHotDeployParameters{" +
            "compileSourcePaths=" + compileSourcePaths +
            ", resourcePaths=" + resourcePaths +
            ", extraPaths=" + extraPaths +
            ", verticleReference='" + verticleReference + '\'' +
            ", configFileName=" + configFileName +
            ", liveHttpReload=" + liveHttpReload +
            ", buildResources=" + buildResources +
            ", notificationPort=" + notificationPort +
            ", buckFile='" + buckFile + '\'' +
            ", buildOutputDirectories='" + buildOutputDirectories + '\'' +
            '}';
  }
}

package io.dazraf.vertx;

import io.dazraf.vertx.compiler.Compiler;

import java.io.File;
import java.util.List;
import java.util.Optional;

public interface HotDeployParameters {

  List<String> getClasspath();

  List<String> getResourcePaths();

  List<String> getCompileSourcePaths();

  String getVerticleReference();

  Optional<String> getConfigFileName();

  boolean isLiveHttpReload();

  boolean isBuildResources();

  int getNotificationPort();

  Optional<List<ExtraPath>> getExtraPaths();

  File getBuildFile();

  List<String> getBuildOutputDirectories();

  Class<? extends Compiler> getCompilerClass();

}

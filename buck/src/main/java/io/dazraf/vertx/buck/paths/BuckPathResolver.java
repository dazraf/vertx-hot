package io.dazraf.vertx.buck.paths;

import io.dazraf.vertx.paths.AbstractPathResolver;
import io.dazraf.vertx.HotDeployParameters;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static java.util.function.Function.identity;
import static java.util.stream.Stream.of;

public class BuckPathResolver extends AbstractPathResolver {

  public BuckPathResolver(HotDeployParameters parameters) {
    super(parameters);
  }

  @Override
  public List<String> getClasspath() {
    List<String> classpath = new ArrayList<>();
    classpath.addAll(parameters.getResourcePaths());
    classpath.addAll(parameters.getBuildOutputDirectories());
    return classpath;
  }

  @Override
  protected Path getPathToProjectRoot() {
    return null;
  }

  @Override
  protected Stream<Path> getCompilableFilePaths() {
    return of(
      parameters.getCompileSourcePaths().stream(),
      getBuildableResources()
    ).flatMap(identity()).map(Paths::get);
  }

}

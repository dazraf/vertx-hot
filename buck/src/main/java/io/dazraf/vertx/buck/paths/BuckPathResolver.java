package io.dazraf.vertx.buck.paths;

import io.dazraf.vertx.paths.AbstractPathResolver;
import io.dazraf.vertx.HotDeployParameters;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Stream.of;

public class BuckPathResolver extends AbstractPathResolver {

  private final Optional<String> projectRootPath;

  public BuckPathResolver(HotDeployParameters parameters, Optional<String> projectRootPath) {
    super(parameters);
    this.projectRootPath = projectRootPath;
  }

  @Override
  public List<String> getClasspath() {
    return of(
      parameters.getResourcePaths().stream(),
      parameters.getBuildOutputDirectories().stream()
    )
      .flatMap(identity())
      .map(Paths::get)
      .map(p -> { return resolveRelativePathToProjectRoot(p); } )
      .map(Path::toString)
      .collect(toList());
  }

  @Override
  public Path getPathToProjectRoot() {
    return Paths.get(projectRootPath.orElse("."));
  }

  @Override
  protected Stream<Path> getCompilableFilePaths() {
    return of(
      parameters.getCompileSourcePaths().stream(),
      getBuildableResources(),
      of(getBuckFilePath().toString())
    )
      .flatMap(identity())
      .map(Paths::get)
      .map(this::resolveRelativePathToProjectRoot);
  }

  private Path getBuckFilePath() {
    return getPathToProjectRoot().resolve("BUCK");
  }

}

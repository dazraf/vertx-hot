package io.dazraf.vertx.buck.paths;

import io.dazraf.vertx.HotDeployParameters;
import io.dazraf.vertx.paths.AbstractPathResolver;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Stream.of;

public class BuckPathResolver extends AbstractPathResolver {

  private final Optional<String> projectRootPath;
  private final String buildTarget;

  public BuckPathResolver(HotDeployParameters parameters, Optional<String> projectRootPath,
                          String buildTarget) {
    super(parameters);
    this.projectRootPath = projectRootPath;
    this.buildTarget = buildTarget;
  }

  @Override
  public List<String> getClasspath() {
    return of(
      parameters.getResourcePaths().stream(),
      parameters.getBuildOutputDirectories().isEmpty() ?
        of(transformBuildTargetToGeneratedJarPath(buildTarget)) :
        parameters.getBuildOutputDirectories().stream()
    )
      .flatMap(identity())
      .map(Paths::get)
      .map(p -> resolveRelativePathToProjectRoot(p))
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
      of("BUCK")
    )
      .flatMap(identity())
      .map(Paths::get)
      .map(this::resolveRelativePathToProjectRoot);
  }

  /**
   * Given a java_binary build target, return the expected project-relative
   * path to the generated jarfile
   *
   * @param buildTarget A Buck build target, such as :app, or //foo/bar/lorem:ipsum. Note
   *          that this should be of java_binary type.
   * @return The expected path, such as buck-out/gen/app.jar or
   *          buck-out/gen/foo/bar/lorem/ipsum.jar
   */
  private String transformBuildTargetToGeneratedJarPath(String buildTarget) {
    StringBuilder sb = new StringBuilder("buck-out/gen/");
    for (String pathComponent: buildTarget.split("/")) {
      if (pathComponent.length() > 0) {
        String[] targetComponents = pathComponent.split(":");
        sb.append(targetComponents[0]).append("/");
        if (targetComponents.length > 1) {
          sb.append(targetComponents[1]).append(".jar");
        }
      }
    }
    return sb.toString();
  }

}

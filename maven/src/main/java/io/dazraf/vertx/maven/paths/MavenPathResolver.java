package io.dazraf.vertx.maven.paths;

import io.dazraf.vertx.paths.AbstractPathResolver;
import io.dazraf.vertx.HotDeployParameters;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Stream;

import static java.util.function.Function.identity;
import static java.util.stream.Stream.of;

public class MavenPathResolver extends AbstractPathResolver {

  private final File buildFile;

  public MavenPathResolver(HotDeployParameters parameters, File buildFile) {
    super(parameters);
    this.buildFile = buildFile;
  }

  @Override
  protected Path getPathToProjectRoot() {
    if (buildFile != null) {
      return buildFile.toPath().getParent();
    } else {
      return null;
    }
  }

  @Override
  protected Stream<Path> getCompilableFilePaths() {
    return of(
            parameters.getCompileSourcePaths().stream(), // set of compile sources
            getBuildableResources(), // the resources
            of(buildFile.getAbsolutePath()) // the pom file itself
    ).flatMap(identity()).map(Paths::get);
  }

  public File getPomFile() {
    return buildFile;
  }

}

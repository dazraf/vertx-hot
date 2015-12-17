package io.dazraf.vertx.paths;

import io.dazraf.vertx.HotDeployParameters;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.stream.Stream.concat;
import static java.util.stream.Stream.empty;

public abstract class AbstractPathResolver implements PathResolver {

  protected final HotDeployParameters parameters;

  private Optional<List<Path>> pathsForCompile = Optional.empty();
  private Optional<List<Path>> pathsForRedeploy = Optional.empty();
  private Optional<List<Path>> pathsForBrowserRefresh = Optional.empty();

  protected AbstractPathResolver(HotDeployParameters parameters) {
    this.parameters = parameters;
  }

  @Override
  public List<String> getClasspath() {
    List<String> classpath = new ArrayList<>();
    classpath.addAll(parameters.getResourcePaths());
    classpath.addAll(parameters.getBuildOutputDirectories());
    return classpath;
  }

  @Override
  public List<Path> pathsThatRequireCompile() {
    return pathsForCompile.orElseGet(() -> // compute and cache the following
      concat(getCompilableFilePaths(), getExtraCompilePaths()).collect(Collectors.toList()) // otherwise return empty list
    );
  }

  @Override
  public List<Path> pathsThatRequireRedeploy() {
    return pathsForRedeploy.orElseGet(() -> // compute and cache the following
      concat(
        getConfigFilePathStream(),
        getExtraRedeployPaths()
      ).collect(Collectors.toList())// otherwise return empty list
    );
  }

  @Override
  public List<Path> pathsThatRequireBrowserRefresh() {
    return pathsForBrowserRefresh.orElseGet(() -> // compute and cache the following
      concat(
        getWatchableResources(),
        getExtraRefreshPaths()
      ).collect(Collectors.toList())// otherwise return empty list
    );
  }

  protected abstract Path getPathToProjectRoot();

  private Stream<Path> getExtraCompilePaths() {
    return parameters.getExtraPaths()
      .map(List::stream) // if we have paths
      .map(stream -> // filter the stream
        stream.filter(ExtraPath::willCauseRecompile) // for stuff that causes a recompile
          .map(ExtraPath::getPath) // get the string path
          .map(Paths::get) // convert to Path
          .map(this::resolveRelativePathToProjectRoot)
        ) // and collect in a list
      .orElse(empty());
  }

  protected abstract Stream<Path> getCompilableFilePaths();

  protected Stream<String> getBuildableResources() {
    if (parameters.isBuildResources()) {
      return parameters.getResourcePaths().stream();
    } else {
      return empty();
    }
  }

  private Stream<Path> getWatchableResources() {
    if (parameters.isLiveHttpReload() && !parameters.isBuildResources()) {
      return parameters.getResourcePaths().stream().map(Paths::get);
    } else {
      return empty();
    }
  }

  private Stream<Path> getExtraRedeployPaths() {
    return parameters.getExtraPaths()
      .map(List::stream) // if we have paths
      .map(stream -> // filter the stream
        stream.filter(ExtraPath::willCauseRedeploy) // for stuff that causes a redeploy
          .map(ExtraPath::getPath) // get the string path
          .map(Paths::get) // convert to Path
          .map(this::resolveRelativePathToProjectRoot) // make absolute
      ) // and collect in a list
      .orElse(empty());
  }

  private Stream<Path> getExtraRefreshPaths() {
    return parameters.getExtraPaths()
      .map(List::stream) // if we have paths
      .map(stream -> // filter the stream
        stream.filter(ExtraPath::willCauseRefresh) // for stuff that causes only a browser refresh
          .map(ExtraPath::getPath) // get the string path
          .map(Paths::get) // convert to Path
          .map(this::resolveRelativePathToProjectRoot) // make absolute
      ) // and collect in a list
      .orElse(empty());
  }

  private Stream<Path> getConfigFilePathStream() {
    return parameters.getConfigFilePath().map(Paths::get).map(this::resolveRelativePathToResourceRoot).map(Stream::of).orElse(empty());
  }

  private Path resolveRelativePathToProjectRoot(Path path) {
    if (path.isAbsolute()) {
      return path;
    } else {
      Path parent = getPathToProjectRoot();
      if (parent != null) {
        return parent.resolve(path);
      }
      return path;
    }
  }

  private Path resolveRelativePathToResourceRoot(Path path) {
    if (path.isAbsolute()) {
      return path;
    }

    return parameters.getResourcePaths().stream()
      // try to find the config file in the resource paths
      .map(Paths::get)
      .map(resourceDirPath -> resourceDirPath.resolve(path))
      .filter(fullPath -> Files.exists(fullPath))
      .findFirst()
      // .. otherwise, append to the first resource path and hope that the config appears there during hot-development
      .orElseGet(() -> parameters.getResourcePaths().stream()
        .map(Paths::get)
        .map(directory -> directory.resolve(path))
        .findFirst()
        // and if we don't have any resources, then just return the path and hope for the best
        .orElse(path));
  }
}

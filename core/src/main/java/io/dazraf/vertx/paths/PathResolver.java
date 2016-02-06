package io.dazraf.vertx.paths;

import java.nio.file.Path;
import java.util.List;

public interface PathResolver {

  List<String> getClasspath();

  List<Path> pathsThatRequireCompile();

  List<Path> pathsThatRequireRedeploy();

  List<Path> pathsThatRequireBrowserRefresh();

}

package io.dazraf.example.buck;

import io.dazraf.vertx.HotDeployParameters;
import io.dazraf.vertx.buck.BuckHotDeployBuilder;
import io.dazraf.vertx.paths.ExtraPath;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Arrays.asList;
import static java.util.stream.Stream.of;

/**
 * Run me with the working directory set to project root.
 *
 * This example builds a classpath of the output library and its
 * prebuilt dependencies.
 *
 */
public class RunExample1WithSeparateLibs {

  public static void main(String [] args) throws Exception {
    final String dir = "example1";
    final String goal = "vertx-hot-example1-lib";
    BuckHotDeployBuilder.create()
      .withBuildTarget(String.format("//%s:%s", dir, goal))
      .withHotDeployConfig(new HotDeployParameters()
        .withVerticleReference("io.dazraf.service.App")
        .withBuildOutputDirectories(
          of(
            findJars(Paths.get(String.format("%s/target/lib", dir))).stream(),
            asList(String.format("buck-out/gen/%s/lib__%s__output/%s.jar", dir, goal, goal)).stream())
          .flatMap(Function.identity())
          .collect(Collectors.toList()))
        .withCompileSourcePaths(asList(
          String.format("%s/src/main/java", dir)))
        .withExtraPaths(asList(
          new ExtraPath()
            .withPath(String.format("%s/src/main/resources", dir))
            .withAction(ExtraPath.VertxHotAction.Recompile),
          new ExtraPath()
            .withPath(String.format("%s/BUCK", dir))
            .withAction(ExtraPath.VertxHotAction.Recompile)))
        .withNotificationPort(9588))
      .build()
      .run();
  }

  private static List<String> findJars(Path path) {
    List<String> jarPaths = new ArrayList<>();
    try (DirectoryStream<Path> dirStream =
           Files.newDirectoryStream(path, "*.{jar}")) {
      dirStream.forEach(p -> jarPaths.add(p.toString()));
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    return jarPaths;
  }

}
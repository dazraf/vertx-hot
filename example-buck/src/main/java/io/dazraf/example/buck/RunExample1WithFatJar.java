package io.dazraf.example.buck;

import io.dazraf.vertx.HotDeployParameters;
import io.dazraf.vertx.buck.BuckHotDeployBuilder;
import io.dazraf.vertx.paths.ExtraPath;

import static java.util.Arrays.asList;

/**
 * Run me with the working directory set to project root.
 *
 * This example is a full fat-jar case, whereby a build occurs on
 * source and resource changes, generating a single fat-jar (java_binary
 * in Buck parlance). The classpath in the final deployment consists
 * of example-buck's (limited) dependencies via the parent classloader,
 * and the fat-jar via an isolated classloader.
 *
 */
public class RunExample1WithFatJar {

  public static void main(String [] args) throws Exception {
    final String dir = "example1";
    final String goal = "vertx-hot-example1";
    BuckHotDeployBuilder.create()
      .withBuildTarget(String.format("//%s:%s", dir, goal))
      .withHotDeployConfig(new HotDeployParameters()
        .withVerticleReference("io.dazraf.service.App")
        .withBuildOutputDirectories(asList(
          String.format("buck-out/gen/%s/%s.jar", dir, goal)))
        .withCompileSourcePaths(asList(String.format("%s/src/main/java", dir)))
        // In this example, buck will compile the resources into the fat jar
        // which we add to the classpath, hence we don't need to configure
        // the resource path (which would also get added to the classpath).
        // The resources should, however, cause the fat jar to be rebuilt.
        .withExtraPaths(asList(new ExtraPath()
          .withPath(String.format("%s/src/main/resources", dir))
          .withAction(ExtraPath.VertxHotAction.Recompile)))
        .withNotificationPort(9588))
      .build()
      .run();
  }

}
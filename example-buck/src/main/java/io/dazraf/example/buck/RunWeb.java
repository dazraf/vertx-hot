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
import java.util.function.Function;
import java.util.stream.Collectors;

import static io.dazraf.vertx.buck.BuckHotDeployBuilder.FetchMode.AUTOMATIC;
import static io.dazraf.vertx.paths.ExtraPath.VertxHotAction.Recompile;
import static java.util.Arrays.asList;
import static java.util.stream.Stream.of;

/**
 * Runs the example-buck-web project in hot-deploy mode.
 *
 * The project is built using Buck, with third-party libraries dynamically
 * retrieved from Maven central using Buck's remote_file support.
 *
 */
public class RunWeb {

  public static void main(String [] args) throws Exception {
    BuckHotDeployBuilder.create()
      .withBuildTarget("//:example-web")
      .withProjectRootPath("../example-buck-web")
      .withFetchMode(AUTOMATIC)
      .withHotDeployConfig(new HotDeployParameters()
        .withVerticleReference("io.dazraf.example.buck.web.App")
        .withBuildOutputDirectories(asList("buck-out/gen/example-web.jar"))
        .withCompileSourcePaths(asList("java"))
        .withExtraPaths(asList(
          new ExtraPath().withPath("res").withAction(Recompile),
          new ExtraPath().withPath("./BUCK").withAction(Recompile)
        ))
        .withNotificationPort(9588)
      )
      .build()
      .run();
  }

}
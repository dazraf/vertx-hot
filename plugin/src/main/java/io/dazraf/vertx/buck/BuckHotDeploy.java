package io.dazraf.vertx.buck;


import io.dazraf.vertx.HotDeploy;

import static java.lang.Boolean.parseBoolean;
import static java.lang.System.getenv;
import static java.util.Arrays.asList;

public class BuckHotDeploy {

  public static void main(String [] args) throws Exception {
    BuckHotDeployParameters params = new BuckHotDeployParameters()
            .withBuckFile(getenv("buckfile"))
            .withBuildOutputDirectories(asList(getenv("buildPath").split(",")))
            .withBuildResources(parseBoolean(getenv("doBuildResources")))
            .withResourcePaths(asList(getenv("resourcePaths").split(",")))
            .withCompileSourcePaths(asList(getenv("sourcePaths").split(",")))
            .withConfigFileName(getenv("vertxConfig"))
            .withNotificationPort(Integer.parseInt(getenv("notificationPort")))
            .withVerticleReference(getenv("verticle"));
    HotDeploy.run(params);
  }

}

package io.dazraf.vertx.maven;


import org.apache.maven.shared.invoker.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.List;


public class Compiler {
  private static final Logger logger = LoggerFactory.getLogger(Compiler.class);

  public static void compile(File pomFile)  {
    InvocationRequest request = new DefaultInvocationRequest();
    request.setPomFile(pomFile);
    List<String> goals = new ArrayList<>();
    goals.add("compile");
    request.setGoals(goals);
    try {
      InvocationResult result = new DefaultInvoker().execute(request);
      if (result.getExitCode() != 0) {
        logger.error("Error with exit code {}", result.getExitCode());
      }
      if (result.getExecutionException() != null) {
        logger.error("Error exit code: " + result.getExitCode(), result.getExecutionException());
      }
    } catch (MavenInvocationException e) {
      logger.error("Maven invocation exception:", e);
    }
  }
}

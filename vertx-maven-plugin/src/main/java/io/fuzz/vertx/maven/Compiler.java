package io.fuzz.vertx.maven;


import org.apache.maven.shared.invoker.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;


public class Compiler {
  private static final Logger logger = LoggerFactory.getLogger(Compiler.class);

  public static void compile()  {
    InvocationRequest request = new DefaultInvocationRequest();
    request.setPomFile(new File(Utils.getCWD() + "/pom.xml"));
    List<String> goals = new ArrayList<>();
    goals.add("compile");
    request.setGoals(goals);
    try {
      InvocationResult result = new DefaultInvoker().execute(request);
      if (result.getExitCode() != 0) {
        logger.info("Exit code {}", result.getExitCode());
      }
    } catch (MavenInvocationException e) {
      logger.error("Maven invocation exception:", e);
    }
  }
}

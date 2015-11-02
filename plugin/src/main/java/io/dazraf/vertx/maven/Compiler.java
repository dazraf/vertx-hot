package io.dazraf.vertx.maven;

import org.apache.maven.shared.invoker.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class Compiler {
  private static final Logger logger = LoggerFactory.getLogger(Compiler.class);
  private static Pattern pattern = Pattern.compile("\\[ERROR\\] [^:]+:\\[\\d+,\\d+\\].*");

  public static void compile(File pomFile) throws Exception {
    InvocationRequest request = new DefaultInvocationRequest();
    request.setPomFile(pomFile);
    Set<String> messages = new HashSet<>();
    StringBuilder output = new StringBuilder();

    request.setOutputHandler(msg -> {
      if (pattern.matcher(msg).matches()) {
        System.out.println(msg);
        messages.add(msg);
      }
    });

    List<String> goals = new ArrayList<>();
    goals.add("compile");
    request.setGoals(goals);
    try {
      InvocationResult result = new DefaultInvoker().execute(request);

      if (result.getExitCode() != 0) {
        String allMessages = messages.stream().collect(Collectors.joining("\n\n"));
        logger.error("Error with exit code {}", result.getExitCode());
        throw new Exception("Compiler failed with exit code: " + result.getExitCode() + "\n" + allMessages);
      }
    } catch (MavenInvocationException e) {
      logger.error("Maven invocation exception:", e);
      throw e;
    }
  }
}

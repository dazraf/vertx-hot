package io.dazraf.vertx.maven;

import org.apache.maven.project.MavenProject;
import org.apache.maven.shared.invoker.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

class Compiler {
  private static final Logger logger = LoggerFactory.getLogger(Compiler.class);
  private static final Pattern ERROR_PATTERN = Pattern.compile("\\[ERROR\\] [^:]+:\\[\\d+,\\d+\\].*");
  private static final Pattern RESOLVE_PATTERN = Pattern.compile("^\\[INFO\\].*:compile:(.*)$");
  private static final List<String> GOALS = Collections.singletonList("dependency:resolve compile");
  private final Properties compilerProperties = new Properties();

  Compiler() {
    compilerProperties.setProperty("outputAbsoluteArtifactFilename", "true");
  }


  List<String> compile(MavenProject project) throws Exception {
    InvocationRequest request = new DefaultInvocationRequest();
    request.setPomFile(project.getFile());

    Set<String> messages = new HashSet<>();
    List<String> classPath = new ArrayList<>();
    classPath.add(project.getBuild().getOutputDirectory());

    request.setOutputHandler(msg -> {
      Matcher matcher = RESOLVE_PATTERN.matcher(msg);
      if (matcher.matches()) {
        String dependency = matcher.group(1);
        classPath.add(dependency);
      }
      if (ERROR_PATTERN.matcher(msg).matches()) {
        System.out.println(msg);
        messages.add(msg);
      }
    });

    request.setGoals(GOALS);
    request.setProperties(compilerProperties);

    try {
      InvocationResult result = new DefaultInvoker().execute(request);

      if (result.getExitCode() != 0) {
        String allMessages = messages.stream().collect(Collectors.joining("\n\n"));

        logger.error("Error with exit code {}", result.getExitCode());
        throw new Exception("Compiler failed with exit code: " + result.getExitCode() + "\n" + allMessages);
      }
      return classPath;
    } catch (MavenInvocationException e) {
      logger.error("Maven invocation exception:", e);
      throw e;
    }
  }
}

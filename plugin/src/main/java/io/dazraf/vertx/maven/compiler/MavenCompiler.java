package io.dazraf.vertx.maven.compiler;

import io.dazraf.vertx.HotDeployParameters;
import io.dazraf.vertx.compiler.CompileResult;
import io.dazraf.vertx.compiler.Compiler;
import io.dazraf.vertx.compiler.CompilerException;
import org.apache.maven.shared.invoker.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This class represents the primary interaction with the Maven runtime to build the project
 */
public class MavenCompiler implements Compiler {
  private static final Logger logger = LoggerFactory.getLogger(MavenCompiler.class);
  private static final Pattern ERROR_PATTERN = Pattern.compile("\\[ERROR\\] [^:]+:\\[\\d+,\\d+\\].*");
  private static final Pattern DEPENDENCY_RESOLUTION_PATTERN = Pattern.compile("^\\[INFO\\].*:compile:(.*)$");
  private static final List<String> GOALS = Collections.singletonList("dependency:resolve compile");
  private final Properties compilerProperties = new Properties();

  public MavenCompiler() {
    compilerProperties.setProperty("outputAbsoluteArtifactFilename", "true");
  }


  /**
   * Compile the maven project, returning the list of classpath paths as reported by maven
   *
   * @param params the deployment parameters
   * @return the result of compilation containing the classpaths etc
   * @throws CompilerException for any compiler errors
   * @throws MavenInvocationException for any unexpected maven invocation errors
   */
  @Override
  public CompileResult compile(HotDeployParameters params) throws CompilerException, MavenInvocationException {
    Set<String> messages = new HashSet<>();
    InvocationRequest request = setupInvocationRequest(params.getBuildFile(), params.getClasspath(), messages);
    return execute(request, messages, params.getClasspath());
  }

  private CompileResult execute(InvocationRequest request, Set<String> messages, List<String> classPath) throws CompilerException, MavenInvocationException  {
    try {
      InvocationResult result = new DefaultInvoker().execute(request);

      if (result.getExitCode() != 0) {
        logger.error("Error with exit code {}", result.getExitCode());
        throw new CompilerException(getClass(), result.getExitCode(), messages);
      }
      return new CompileResult(classPath);
    } catch (MavenInvocationException e) {
      logger.error("Maven invocation exception:", e);
      throw e;
    }
  }

  private InvocationRequest setupInvocationRequest(File buildFile, List<String> classPath, Set<String> messages) {
    InvocationRequest request = new DefaultInvocationRequest();
    request.setPomFile(buildFile);

    request.setOutputHandler(msg -> collectResults(msg, messages, classPath));

    request.setGoals(GOALS);
    request.setProperties(compilerProperties);
    return request;
  }

  private void collectResults(String msg, Set<String> messages, List<String> classPath) {
    Matcher matcher = DEPENDENCY_RESOLUTION_PATTERN.matcher(msg);
    if (matcher.matches()) {
      String dependency = matcher.group(1);
      classPath.add(dependency);
    }
    if (ERROR_PATTERN.matcher(msg).matches()) {
      System.out.println(msg);
      messages.add(msg);
    }
  }
}

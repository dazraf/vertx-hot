package io.dazraf.vertx.maven.compiler;

import io.dazraf.vertx.compiler.CompileResult;
import io.dazraf.vertx.compiler.Compiler;
import io.dazraf.vertx.compiler.CompilerException;
import io.dazraf.vertx.maven.paths.MavenPathResolver;
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

  private static final Logger LOGGER = LoggerFactory.getLogger(Compiler.class);
  private static final Pattern ERROR_PATTERN = Pattern.compile("\\[ERROR\\] [^:]+:\\[\\d+,\\d+\\].*");
  private static final Pattern DEPENDENCY_RESOLUTION_PATTERN = Pattern.compile("^\\[INFO\\].*:compile:(.*)$");
  private static final List<String> GOALS = Collections.singletonList("dependency:resolve compile");

  private final Properties compilerProperties = new Properties();
  private final MavenPathResolver pathResolver;

  public MavenCompiler(MavenPathResolver pathResolver) {
    this.pathResolver = pathResolver;
    this.compilerProperties.setProperty("outputAbsoluteArtifactFilename", "true");
  }

  /**
   * Compile the maven project, returning the list of classpath paths as reported by maven
   *  
   * @return the result of compilation containing the classpaths etc
   * @throws CompilerException for any compiler errors
   * @throws MavenInvocationException for any unexpected maven invocation errors
   */
  @Override
  public CompileResult compile() throws CompilerException, MavenInvocationException {
    Set<String> messages = new HashSet<>();
    InvocationRequest request = setupInvocationRequest(pathResolver.getPomFile(),
            pathResolver.getClasspath(), messages);
    return execute(request, messages, pathResolver.getClasspath());
  }

  private CompileResult execute(InvocationRequest request, Set<String> messages, List<String> classPath) throws CompilerException, MavenInvocationException  {
    try {
      InvocationResult result = new DefaultInvoker().execute(request);

      if (result.getExitCode() != 0) {
        LOGGER.error("Error with exit code {}", result.getExitCode());
        throw new CompilerException(getClass(), result.getExitCode(), messages);
      }
      return new CompileResult(classPath);
    } catch (MavenInvocationException e) {
      LOGGER.error("Maven invocation exception:", e);
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
    } else if (ERROR_PATTERN.matcher(msg).matches()) {
      System.out.println(msg);
      messages.add(msg);
    } else {
      LOGGER.trace("> {}", msg);
    }
  }
}

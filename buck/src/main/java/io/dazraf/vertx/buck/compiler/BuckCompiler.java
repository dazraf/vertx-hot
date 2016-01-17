package io.dazraf.vertx.buck.compiler;

import io.dazraf.vertx.buck.BuckHotDeployBuilder;
import io.dazraf.vertx.buck.paths.BuckPathResolver;
import io.dazraf.vertx.compiler.CompileResult;
import io.dazraf.vertx.compiler.Compiler;
import io.dazraf.vertx.compiler.CompilerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import static java.util.Arrays.asList;

public class BuckCompiler implements Compiler {

  private static final Logger log = LoggerFactory.getLogger(BuckCompiler.class);

  private final String buildTarget;
  private final BuckPathResolver pathResolver;
  private final BuckHotDeployBuilder.FetchMode fetchMode;

  public BuckCompiler(String buildTarget, BuckHotDeployBuilder.FetchMode fetchMode, BuckPathResolver pathResolver) {
    this.buildTarget = buildTarget;
    this.fetchMode = fetchMode;
    this.pathResolver = pathResolver;
  }

  @Override
  public CompileResult compile() throws Exception {
    if (BuckHotDeployBuilder.FetchMode.AUTOMATIC.equals(fetchMode)) {
      runProcess(5, TimeUnit.MINUTES, "buck", "fetch", buildTarget);
    }
    return runProcess(2, TimeUnit.MINUTES, "buck", "build", buildTarget);
  }

  private CompileResult runProcess(long timeoutValue, TimeUnit timeoutUnit, String... commands)
      throws IOException, InterruptedException, CompilerException {
    log.info("Running \"{}\"", String.join(" ", commands));
    Process process = new ProcessBuilder()
      .command(commands)
      .directory(pathResolver.getPathToProjectRoot().toFile())
      .redirectOutput(ProcessBuilder.Redirect.INHERIT)
      .redirectError(ProcessBuilder.Redirect.INHERIT)
      .start();
    if (process.waitFor(timeoutValue, timeoutUnit)) {
      int ret = process.exitValue();
      if (ret == 0) {
        return new CompileResult(pathResolver.getClasspath());
      } else {
        throw new CompilerException(getClass(), ret,
          asList(String.format("\"%s\" failed", String.join(" ", commands))));
      }
    } else {
      process.destroyForcibly();
      throw new CompilerException(getClass(), -1,
        asList(String.format("\"%s\" exceeded its timeout", String.join(" ", commands))));
    }
  }

}

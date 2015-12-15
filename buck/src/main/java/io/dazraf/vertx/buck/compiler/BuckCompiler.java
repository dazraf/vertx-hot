package io.dazraf.vertx.buck.compiler;

import io.dazraf.vertx.HotDeployParameters;
import io.dazraf.vertx.buck.BuckHotDeployParameters;
import io.dazraf.vertx.compiler.CompileResult;
import io.dazraf.vertx.compiler.Compiler;
import io.dazraf.vertx.compiler.CompilerException;

import java.util.concurrent.TimeUnit;

import static java.util.Arrays.asList;

public class BuckCompiler implements Compiler {

  @Override
  public CompileResult compile(HotDeployParameters params) throws Exception {
    BuckHotDeployParameters buckParams = (BuckHotDeployParameters) params;
    Process process = new ProcessBuilder()
            .command("buck", "build", buckParams.getBuildTarget())
            .redirectOutput(ProcessBuilder.Redirect.INHERIT)
            .redirectError(ProcessBuilder.Redirect.INHERIT)
            .start();
    if (process.waitFor(60, TimeUnit.SECONDS)) {
      int ret = process.exitValue();
      if (ret == 0) {
        return new CompileResult(params.getClasspath());
      } else {
        throw new CompilerException(getClass(), ret, asList("buck build failed"));
      }
    } else {
      process.destroyForcibly();
      throw new CompilerException(getClass(), -1, asList("buck build exceeded its timeout"));
    }
  }

}

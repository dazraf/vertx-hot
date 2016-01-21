package io.dazraf.vertx.buck.compiler;

import io.dazraf.vertx.HotDeployParameters;
import io.dazraf.vertx.buck.BuckHotDeployBuilder;
import io.dazraf.vertx.buck.paths.BuckPathResolver;
import io.dazraf.vertx.compiler.CompileResult;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import static java.util.Arrays.asList;
import static java.util.Optional.ofNullable;
import static org.junit.Assert.assertEquals;
import static org.junit.Assume.assumeTrue;

/**
 * Some systems won't have buck installed. That's OK: vertx-hot
 * should still build in this case. To support this, these tests
 * are conditional; they only run if the buck binary is available
 * in the executing environment.
 *
 * Ideally we'd have a test dependency on the buck binary so that
 * execution can be guaranteed in all environments.
 */
public class BuckCompilerTest {

  @Before
  public void proceedOnlyIfBuckBinaryExists() throws IOException, InterruptedException {
    Process proc = new ProcessBuilder()
      .command("buck", "--version")
      .start();
    // If buck is present, it will execute, report its version to stdout
    // and return 0.
    if (!proc.waitFor(30, TimeUnit.SECONDS)) {
      proc.destroyForcibly();
    }
    assumeTrue(!proc.isAlive() && proc.exitValue() == 0);
  }

  @Test
  public void build() throws Exception {
    final BuckPathResolver pathResolver = new BuckPathResolver(
      new HotDeployParameters()
        .withVerticleReference("io.dazraf.vertx.buck.test.App")
        .withBuildOutputDirectories(asList("buck-out/gen/test-project.jar"))
        .withCompileSourcePaths(asList("java")),
      ofNullable("target/test-classes/project")
    );
    final BuckCompiler compiler = new BuckCompiler(
      "//:test-project",
      BuckHotDeployBuilder.FetchMode.MANUAL,
      pathResolver
    );
    final CompileResult actual = compiler.compile();
    assertEquals(pathResolver.getClasspath(), actual.getClassPath());
  }

}

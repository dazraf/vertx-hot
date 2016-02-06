package io.dazraf.vertx.buck.compiler;

import io.dazraf.vertx.HotDeployParameters;
import io.dazraf.vertx.buck.BuckHotDeployBuilder;
import io.dazraf.vertx.buck.DependentOnBuckBinary;
import io.dazraf.vertx.buck.paths.BuckPathResolver;
import io.dazraf.vertx.compiler.CompileResult;
import org.junit.Test;

import static java.util.Collections.singletonList;
import static java.util.Optional.of;
import static org.junit.Assert.assertEquals;

public class BuckCompilerTest extends DependentOnBuckBinary {

  @Test
  public void build() throws Exception {
    final BuckPathResolver pathResolver = new BuckPathResolver(
      new HotDeployParameters()
        .withVerticleReference("io.dazraf.vertx.buck.test.App")
        .withBuildOutputDirectories(singletonList("buck-out/gen/test-project.jar"))
        .withCompileSourcePaths(singletonList("java")),
      of("target/test-classes/project"),
      "//:test-project"
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

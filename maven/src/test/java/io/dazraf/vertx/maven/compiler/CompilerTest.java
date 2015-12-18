package io.dazraf.vertx.maven.compiler;

import io.dazraf.vertx.HotDeployParameters;
import io.dazraf.vertx.compiler.CompileResult;
import io.dazraf.vertx.compiler.CompilerException;
import io.dazraf.vertx.maven.paths.MavenPathResolver;
import org.apache.maven.shared.invoker.MavenInvocationException;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.regex.Pattern;

import static java.util.Collections.singletonList;
import static org.junit.Assert.assertTrue;

public class CompilerTest {
  private static final Logger LOGGER = LoggerFactory.getLogger(CompilerTest.class);

  @Test
  public void testCompileExample1() throws CompilerException, MavenInvocationException, IOException {
    File projectFile = new File("../example1/pom.xml").getAbsoluteFile();
    String buildOutputDir = projectFile.getParentFile().toPath().resolve("target/classes").toFile().getCanonicalPath();
    MavenPathResolver pathResolver = new MavenPathResolver(
      new HotDeployParameters().withBuildOutputDirectories(
        singletonList(buildOutputDir)
      ),
      projectFile);
    MavenCompiler compiler = new MavenCompiler(pathResolver);
    final CompileResult compileResult = compiler.compile();
    Pattern pattern = Pattern.compile("(^.+\\.jar$)|(^.+/target/classes$)");
    assertTrue(compileResult.getClassPath().size() > 0);
    compileResult.getClassPath().stream().forEach(path -> {
      LOGGER.info("checking: {}", path);
      assertTrue(pattern.matcher(path).matches());
    });

    assertTrue(compileResult.getClassPath().contains(buildOutputDir));
  }
}

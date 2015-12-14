package io.dazraf.vertx.maven;

import io.dazraf.vertx.maven.compiler.CompileResult;
import io.dazraf.vertx.maven.compiler.Compiler;
import io.dazraf.vertx.maven.compiler.CompilerException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.shared.invoker.MavenInvocationException;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.regex.Pattern;

import static org.junit.Assert.assertTrue;

public class CompilerTest {
  private static final Logger LOGGER = LoggerFactory.getLogger(CompilerTest.class);

  @Test
  public void testCompileExample1() throws CompilerException, MavenInvocationException, IOException {
    File projectFile = new File("../example1/pom.xml").getAbsoluteFile();
    Compiler compiler = new Compiler();
    MavenProject project = new MavenProject();
    project.setFile(projectFile);
    project.getBuild().setOutputDirectory(projectFile.getParentFile().toPath().resolve("target/classes").toFile().getCanonicalPath());
    final CompileResult compileResult = compiler.compile(project);
    Pattern pattern = Pattern.compile("(^.+\\.jar$)|(^.+/target$)");
    assertTrue(compileResult.getClassPath().size() > 0);
    compileResult.getClassPath().stream().forEach(path -> {
      LOGGER.info("checking: {}", path);
      assertTrue(pattern.matcher(path).matches());
    });

    assertTrue(compileResult.getClassPath().contains(project.getBuild().getOutputDirectory()));
  }
}

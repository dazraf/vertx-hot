package io.dazraf.vertx.compiler;

import java.util.List;

public class CompileResult {
  private final List<String> classPath;

  public CompileResult(List<String> classPath) {
    this.classPath = classPath;
  }

  public List<String> getClassPath() {
    return classPath;
  }
}

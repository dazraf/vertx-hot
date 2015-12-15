package io.dazraf.vertx.compiler;

import java.util.Collection;
import java.util.stream.Collectors;

public class CompilerException extends Exception {
  private final Collection<String> messages;
  private final int exitCode;

  public CompilerException(Class<?> compilerClass, int exitCode, Collection<String> messages) {
    super(compilerClass.getSimpleName() + " failed with exit code: " + exitCode + "\n" +
      messages.stream().collect(Collectors.joining("\n\n")));
    this.exitCode = exitCode;
    this.messages = messages;
  }

  public Collection<String> getMessages() {
    return messages;
  }

  public int getExitCode() {
    return exitCode;
  }
}

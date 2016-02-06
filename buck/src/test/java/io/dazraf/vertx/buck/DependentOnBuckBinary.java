package io.dazraf.vertx.buck;


import org.junit.Before;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import static org.junit.Assume.assumeNoException;
import static org.junit.Assume.assumeTrue;

/**
 * Some systems won't have buck installed. That's OK: vertx-hot
 * should still build in this case. To support this, these tests
 * are conditional; they only run if the buck binary is available
 * in the executing environment.
 *
 * Unfortunately (but not unsurprisingly) Travis CI doesn't provide
 * the option of adding Buck to the environment, so these tests
 * won't be run on the CI server - yet.
 */
public abstract class DependentOnBuckBinary {

  @Before
  public void proceedOnlyIfBuckBinaryExists() throws InterruptedException {
    try {
      Process proc = new ProcessBuilder()
        .command("buck", "--version")
        .start();
      // If buck is present, it will execute, report its version to stdout
      // and return 0.
      if (!proc.waitFor(30, TimeUnit.SECONDS)) {
        proc.destroyForcibly();
      }
      assumeTrue(!proc.isAlive() && proc.exitValue() == 0);
    } catch (IOException e) {
      assumeNoException(e);
    }
  }

}

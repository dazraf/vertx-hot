package io.dazraf.vertx.deployer;

import java.util.Objects;
import java.util.Optional;

public class DeploymentResult {

  private String verticleName;
  private Optional<String> verticleId;
  private Optional<Throwable> cause;

  public static DeploymentResult success(String verticleName, String verticleId) {
    return new DeploymentResult(verticleName, Optional.of(verticleId), Optional.empty());
  }

  public static DeploymentResult failure(String verticleName, Throwable cause) {
    return new DeploymentResult(verticleName, Optional.empty(), Optional.of(cause));
  }

  private DeploymentResult(String verticleName, Optional<String> verticleId, Optional<Throwable> cause) {
    this.verticleName = verticleName;
    this.verticleId = verticleId;
    this.cause = cause;
  }

  public String getVerticleName() {
    return verticleName;
  }

  public Optional<String> getVerticleId() {
    return verticleId;
  }

  public Optional<Throwable> getCause() {
    return cause;
  }

  public boolean isSuccess() {
    return verticleId.isPresent();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    DeploymentResult that = (DeploymentResult) o;
    return Objects.equals(verticleName, that.verticleName) &&
      Objects.equals(verticleId, that.verticleId) &&
      Objects.equals(cause, that.cause);
  }

  @Override
  public int hashCode() {
    return Objects.hash(verticleName, verticleId, cause);
  }

  @Override
  public String toString() {
    return String.format("%20s: %s",
      getShortVerticleName(),
      (verticleId.isPresent() ? "RUNNING: " + verticleId.get() : "FAILED: " + cause.get())
    );
  }

  private String getShortVerticleName() {
      return verticleName.lastIndexOf(".") > 0 ?
        verticleName.substring(verticleName.lastIndexOf(".") + 1) : verticleName;
  }
}

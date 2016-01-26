package io.dazraf.vertx.buck;

import io.dazraf.vertx.*;
import io.dazraf.vertx.buck.compiler.BuckCompiler;
import io.dazraf.vertx.buck.paths.BuckPathResolver;
import io.dazraf.vertx.deployer.GenericVerticleDeployer;

import java.util.Optional;

import static java.util.Optional.ofNullable;

public class BuckHotDeployBuilder {

  public static BuckHotDeployBuilder create() {
    return new BuckHotDeployBuilder();
  }

  private String buildTarget;
  private String projectRootPath;
  private FetchMode fetchMode = FetchMode.MANUAL;

  private HotDeployParameters parameters;

  public enum FetchMode {
    MANUAL,
    AUTOMATIC
  }

  private BuckHotDeployBuilder() {}

  public BuckHotDeployBuilder withBuildTarget(String buildTarget) {
    this.buildTarget = buildTarget;
    return this;
  }

  public BuckHotDeployBuilder withFetchMode(FetchMode fetchMode) {
    if (fetchMode != null) {
      this.fetchMode = fetchMode;
    }
    return this;
  }

  public BuckHotDeployBuilder withProjectRootPath(String projectRootPath) {
    this.projectRootPath = projectRootPath;
    return this;
  }

  public BuckHotDeployBuilder withHotDeployConfig(HotDeployParameters parameters) {
    this.parameters = parameters;
    return this;
  }

  public HotDeploy build() {
    BuckPathResolver pathResolver = new BuckPathResolver(parameters, ofNullable(projectRootPath), buildTarget);
    BuckCompiler compiler = new BuckCompiler(buildTarget, fetchMode, pathResolver);
    GenericVerticleDeployer verticleDeployer = new GenericVerticleDeployer(parameters);
    return new HotDeploy(compiler, verticleDeployer, pathResolver, parameters.getShutdownCondition());
  }

}

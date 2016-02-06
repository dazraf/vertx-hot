package io.dazraf.vertx.maven;

import io.dazraf.vertx.HotDeploy;
import io.dazraf.vertx.HotDeployParameters;
import io.dazraf.vertx.deployer.GenericVerticleDeployer;
import io.dazraf.vertx.deployer.VerticleDeployer;
import io.dazraf.vertx.maven.compiler.MavenCompiler;
import io.dazraf.vertx.maven.paths.MavenPathResolver;

import java.io.File;

public class MavenHotDeployBuilder {

  private File buildFile;

  public static MavenHotDeployBuilder create() {
    return new MavenHotDeployBuilder();
  }

  private HotDeployParameters hotDeployParameters;

  public MavenHotDeployBuilder withHotDeployParameters(HotDeployParameters hotDeployParameters) {
    this.hotDeployParameters = hotDeployParameters;
    return this;
  }
  
  public MavenHotDeployBuilder withBuildFile(File buildFile) {
    this.buildFile = buildFile;
    return this;
  }
  
  public HotDeploy build() {
    MavenPathResolver pathResolver = new MavenPathResolver(hotDeployParameters, buildFile);
    MavenCompiler compiler = new MavenCompiler(pathResolver);
    VerticleDeployer verticleDeployer = new GenericVerticleDeployer(hotDeployParameters);
    return new HotDeploy(compiler, verticleDeployer, pathResolver, hotDeployParameters.getShutdownCondition());
  }

}

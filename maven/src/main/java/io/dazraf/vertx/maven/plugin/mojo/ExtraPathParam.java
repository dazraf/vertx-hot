package io.dazraf.vertx.maven.plugin.mojo;

import static io.dazraf.vertx.ExtraPath.VertxHotAction;

import io.dazraf.vertx.ExtraPath;
import org.apache.maven.plugins.annotations.Parameter;

public class ExtraPathParam {

  @Parameter(name = "path", required = true)
  private String path;

  @Parameter(name = "action", defaultValue = "Redeploy", required = false)
  private VertxHotAction action = VertxHotAction.Redeploy;

  public ExtraPath getExtraPath() {
    return new ExtraPath().withPath(path).withAction(action);
  }

  public ExtraPathParam withPath(String path) {
    this.path = path;
    return this;
  }

  public ExtraPathParam withAction(VertxHotAction vertxHotAction) {
    this.action = vertxHotAction;
    return this;
  }

}

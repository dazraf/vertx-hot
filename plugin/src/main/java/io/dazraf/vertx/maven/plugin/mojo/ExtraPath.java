package io.dazraf.vertx.maven.plugin.mojo;

import org.apache.maven.plugins.annotations.Parameter;

public class ExtraPath {

  public enum VertxHotAction {
    Nothing,
    Refresh,
    Redeploy,
    Recompile
  }

  public ExtraPath() {
  }

  @Parameter(name = "path", required = true)
  private String path;

  @Parameter(name = "action", defaultValue = "Redeploy", required = false)
  private VertxHotAction action = VertxHotAction.Redeploy;

  public String getPath() {
    return path;
  }

  public ExtraPath withPath(String path) {
    this.path = path;
    return this;
  }

  public ExtraPath withAction(VertxHotAction vertxHotAction) {
    this.action = vertxHotAction;
    return this;
  }

  public boolean willCauseRecompile() {
    return action == VertxHotAction.Recompile;
  }

  public boolean willCauseRedeploy() {
    return action == VertxHotAction.Redeploy;
  }

  public boolean willCauseRefresh() { return action == VertxHotAction.Refresh; }
}

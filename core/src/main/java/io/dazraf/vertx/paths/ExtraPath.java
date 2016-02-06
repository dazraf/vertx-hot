package io.dazraf.vertx.paths;


public class ExtraPath {

  public static enum VertxHotAction {
    Nothing,
    Refresh,
    Redeploy,
    Recompile
  }

  private String path;
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

  @Override
  public String toString() {
    return "ExtraPath{" +
            "path='" + path + '\'' +
            ", action=" + action +
            '}';
  }

}

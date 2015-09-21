package io.fuzz.service;

import io.fuzz.vertx.maven.HotDeploy;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.http.HttpServer;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class App extends AbstractVerticle {
  private static final Logger logger = LoggerFactory.getLogger(App.class);
  private HttpServer server;

  // Convenience method so you can run it in your IDE
  public static void main(String[] args) throws Exception {
    HotDeploy.run("io.fuzz.service.App");
    // new VertxManager().deploy(App.class);
  }

  @Override
  public void start() {
    int port = config().getInteger("port", 8080);
    logger.info("Starting server on port {}", port);
    Router router = Routes.create(vertx, this);
    this.server  = vertx.createHttpServer().requestHandler(router::accept).listen(port);
    System.out.println("Server is started");
  }

  @Override
  public void stop() throws Exception {
    logger.info("stopping");
    server.close();
    super.stop();
  }

  public void test(RoutingContext context) {
    context.response().end("<html><body><h2>Description</h2>this is a simple result that tells the story</body></html>");
  }
}
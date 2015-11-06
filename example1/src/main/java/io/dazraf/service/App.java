package io.dazraf.service;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServer;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.Scanner;

import static io.dazraf.service.utils.routing.RouteMaster.bindHandlebarTemplates;
import static io.dazraf.service.utils.routing.RouteMaster.bindStatic;

public class App extends AbstractVerticle {
  private static final Logger logger = LoggerFactory.getLogger(App.class);
  private HttpServer server;
  private AppController appController;

  // Convenience method so you can run it in your IDE
  public static void main(String[] args) throws Exception {
    Vertx vertx = Vertx.vertx();
    vertx.deployVerticle(App.class.getCanonicalName(), new DeploymentOptions().setConfig(loadConfig()));
  }

  private static JsonObject loadConfig() throws IOException {
    JsonObject config;
    try (InputStream resourceAsStream = Thread.currentThread().getContextClassLoader().getResourceAsStream("config.json")) {
      try (Scanner scanner = new Scanner(resourceAsStream)) {
        config = new JsonObject(scanner.useDelimiter("\\A").next());
      }
    }
    return config;
  }

  @Override
  public void start() throws InterruptedException, IOException {
    int port = config().getInteger("port", 8080);
    // this flag gets set by vertx:hot
    boolean devMode = config().getBoolean("devmode");
    logger.info("Dev mode: {}", devMode);

    appController = new AppController();

    logger.info("Starting server on port {}", port);
    Router router = createRoutes(vertx);

    this.server = vertx
      .createHttpServer()
      .requestHandler(router::accept)
      .listen(port, ar -> {
        if (ar.succeeded()) {
          this.server = ar.result();
          logger.info("Server started on port {}", port);
          logger.info("Browse to: http://localhost:{}", port);
        } else {
          logger.error("Failed to start up: ", ar.cause());
        }
      });
  }

  @Override
  public void stop() throws Exception {
    logger.info("stopping");
    appController.close();
    server.close();
  }

  private Router createRoutes(Vertx vertx) {
    Router router = Router.router(vertx);
    bindStatic(router, "/components/*", "bower_components");
    router.get("/api/test").handler(appController::createStory);
    bindHandlebarTemplates(router, appController, "/dynamic", "templates");
    bindStatic(router, "static");
    return router;
  }
}
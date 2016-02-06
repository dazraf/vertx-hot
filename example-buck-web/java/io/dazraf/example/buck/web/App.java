package io.dazraf.example.buck.web;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServer;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.StaticHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class App extends AbstractVerticle {
  private static final Logger logger = LoggerFactory.getLogger(App.class);
  private HttpServer server;
  private NewsController newsController;

  @Override
  public void start() throws InterruptedException, IOException {
    int port = config().getInteger("port", 8080);
    logger.info("Starting server on port {}", port);

    newsController = new NewsController();
    Router router = createRoutes(vertx);

    this.server = vertx
      .createHttpServer()
      .requestHandler(router::accept)
      .listen(port, ar -> {
        if (ar.succeeded()) {
          this.server = ar.result();
          logger.info("Server started. Browse to: http://localhost:{}", port);
        } else {
          logger.error("Failed to start: ", ar.cause());
        }
      });
  }

  @Override
  public void stop() throws Exception {
    logger.info("stopping");
    server.close();
  }

  private Router createRoutes(Vertx vertx) {
    Router router = Router.router(vertx);
    router.get("/api/article")
      .produces("application/json")
      .handler(newsController::randomArticle);
    router.route("/*")
      .handler(StaticHandler
        .create()
        .setWebRoot("static"));
    return router;
  }

}

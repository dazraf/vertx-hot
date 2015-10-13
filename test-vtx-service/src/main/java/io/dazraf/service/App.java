package io.dazraf.service;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServer;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class App extends AbstractVerticle {
  private static final Logger logger = LoggerFactory.getLogger(App.class);
  private HttpServer server;
  private boolean flag = false;

  // Convenience method so you can run it in your IDE
  public static void main(String[] args) throws Exception {
    Vertx vertx = Vertx.vertx();
    vertx.deployVerticle(App.class.getCanonicalName());
  }

  @Override
  public void start() throws InterruptedException {
    int port = config().getInteger("port", 8080);
    logger.info("Deploying child service");
    getVertx().deployVerticle(new ChildService(), ar -> {
      if (ar.succeeded()) {
        logger.info("Child service deployed: {}", ar.result());
      } else {
        logger.error("Failed to deploy child service");
      }
    });
    logger.info("Starting server on port {}", port);
    Router router = Routes.create(vertx, this);
    this.server = vertx.createHttpServer().requestHandler(router::accept).listen(port);
    logger.info("Server is started on port {}", port);
    logger.info("Browse to: http://localhost:{}", port);
  }

  @Override
  public void stop() throws Exception {
    logger.info("stopping");
    server.close();
    super.stop();
  }

  public void test(RoutingContext context) {
    context.response().setChunked(true);
    context.response().write(
      "<!DOCTYPE html>" +
        "<html lang=\"en\">" +
        "<head>" +
        "    <title>Index</title>" +
        "    <link rel=\"stylesheet\" href=\"components/bootstrap/dist/css/bootstrap.min.css\">" +
        "    <link rel=\"stylesheet\" href=\"http://bootswatch.com/paper/bootstrap.min.css\"/>" +
        "</head>" +
        "<body>" +
        "<div class=\"jumbotron\">" +
        "<div class=\"container\">" +
        "<h2>Try Refresh ... </h2>"
    );
    if (!flag) {
      context.response().write("<p>This is a simple result that tells the story</p>");
    } else {
      context.response().write("<p>This is another story</p>");
    }
    context.response().end(
      "<a class=\"btn btn-primary\" href=\"/\" role=\"button\">Go Back</a>" +
        "</div></div>" +
        "<script src=\"components/jquery/dist/jquery.min.js\"></script>" +
        "<script src=\"components/bootstrap/dist/js/bootstrap.min.js\"></script>" +
        "</body>" +
        "</html>");
    flag = !flag;
  }
}
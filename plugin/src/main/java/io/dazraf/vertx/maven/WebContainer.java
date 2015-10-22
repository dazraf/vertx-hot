package io.dazraf.vertx.maven;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.eventbus.MessageConsumer;
import io.vertx.core.http.HttpServer;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.StaticHandler;
import io.vertx.ext.web.templ.HandlebarsTemplateEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WebContainer extends AbstractVerticle {
  public static final String TOPIC = "vertx.hot.status";
  private static final Logger logger = LoggerFactory.getLogger(WebContainer.class);
  private HttpServer httpServer;
  private MessageConsumer<JsonObject> consumer;
  private JsonObject lastState;

  @Override
  public void start() throws Exception {
    httpServer = vertx.createHttpServer();
    HandlebarsTemplateEngine engine = HandlebarsTemplateEngine.create();
    engine.setMaxCacheSize(0);

    consumer = vertx.eventBus().consumer(TOPIC);
    consumer.handler(m -> {
      lastState = m.body();
    });

    httpServer
      .websocketHandler(websocketHandler -> {
          if (!websocketHandler.path().equals("/vertx/hot")) {
            websocketHandler.reject();
            return;
          }
          MessageConsumer<JsonObject> consumer = vertx.eventBus().consumer(TOPIC);
          consumer.handler(m -> {
            websocketHandler.writeFinalTextFrame(m.body().toString());
          });
          websocketHandler.closeHandler((v) -> consumer.unregister());
        }
      )
      .listen(9999);
    logger.info("proxy service started on: http://localhost:{}", 9999);
  }

  @Override
  public void stop() throws Exception {
    consumer.unregister();
    httpServer.close();
  }

  public void bindStatic(Router router, String urlPath, String localPath) {
    router.route(urlPath)
      .handler(StaticHandler
        .create()
        .setWebRoot(localPath));
  }
}

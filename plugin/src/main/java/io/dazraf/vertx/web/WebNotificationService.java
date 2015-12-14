package io.dazraf.vertx.web;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.eventbus.MessageConsumer;
import io.vertx.core.http.HttpServer;
import io.vertx.core.json.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WebNotificationService extends AbstractVerticle {
  public static final String TOPIC = "vertx.hot.status";
  private static final Logger logger = LoggerFactory.getLogger(WebNotificationService.class);
  private final int notificationPort;
  private HttpServer httpServer;

  public WebNotificationService(int notificationPort) {
    this.notificationPort = notificationPort;
  }

  @Override
  public void start() throws Exception {
    httpServer = vertx.createHttpServer();
    httpServer
      .websocketHandler(websocketHandler -> {
          if (!websocketHandler.path().equals("/vertx/hot")) {
            websocketHandler.reject();
            return;
          }
          MessageConsumer<JsonObject> consumer = vertx.eventBus().consumer(TOPIC);
          consumer.handler(m -> websocketHandler.writeFinalTextFrame(m.body().toString()));
          websocketHandler.closeHandler((v) -> consumer.unregister());
        }
      )
      .listen(notificationPort);
    logger.info("notification websocket started on: http://localhost:{}", notificationPort);
  }

  @Override
  public void stop() throws Exception {
    httpServer.close();
  }
}

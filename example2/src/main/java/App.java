import io.vertx.core.AbstractVerticle;
import io.vertx.core.eventbus.MessageConsumer;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.mongo.MongoClient;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.StaticHandler;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class App extends AbstractVerticle {
  private static final Logger logger = LoggerFactory.getLogger(App.class);
  private static final String TASK_DB = "TaskApp";
  private static final java.lang.String TASK_COLLECTION = "Tasks";
  private static final String WEB_SOCKET_PORT = "/api/notifications";
  private static final String NOTIFICATION_TOPIC = "task.notification";
  private HttpServer httpServer;
  private MongoClient db;

  private enum Operation {
    Create,
    Delete,
    Update
  }

  @Override
  public void start() throws Exception {
    setupDatabaseClient();
    setWebServer();
    logger.info("started");
  }

  private void setupDatabaseClient() {
    this.db = MongoClient.createNonShared(vertx, config().getJsonObject("db"));
  }

  private void setWebServer() {
    int webPort = config().getInteger("port", 8080);
    Router router = Router.router(vertx);
    router.route().handler(BodyHandler.create());
    bindApi(router);
    bindBowerComponents(router);
    bindStaticFiles(router);

    this.httpServer = vertx.createHttpServer()
      .requestHandler(router::accept)
      .websocketHandler(webSocket -> {
        if (webSocket.path().equals(WEB_SOCKET_PORT)) {
          MessageConsumer<JsonObject> consumer = vertx.eventBus().consumer(NOTIFICATION_TOPIC);
          consumer.handler(msg -> webSocket.writeFinalTextFrame(msg.body().toString()));
          webSocket.closeHandler((v) -> consumer.unregister());
        } else {
          webSocket.reject();
        }
      })
      .listen(webPort, ar -> {
        if (ar.failed()) {
          logger.error("failed to start", ar.cause());
        } else {
          logger.info("server started on: http://localhost:" + webPort);
        }
      });
  }

  private void bindApi(Router router) {
    bindGetTasks(router);
    bindGetTask(router);
    bindCreateTask(router);
    bindUpdateTask(router);
    bindDeleteTask(router);
  }

  private void bindGetTasks(Router router) {
    router.get("/api/task").handler(rc -> {
      HttpServerResponse response = rc.response();
      db.find(TASK_COLLECTION, new JsonObject(), ar -> {
        if (ar.failed()) {
          reportError(response, ar.cause());
        } else {
          response.putHeader("content-type", rc.getAcceptableContentType());
          JsonArray result = new JsonArray(ar.result());
          response.end(result.toString());
        }
      });
    }).produces("application/json");
  }

  private void reportError(HttpServerResponse response, Throwable cause) {
    response.setStatusCode(500).setStatusMessage(cause.getMessage()).end();
  }

  private void bindGetTask(Router router) {
    router.get("/api/task/:id").handler(rc -> {
      String id = rc.request().getParam("id");
      HttpServerResponse response = rc.response();

      JsonObject query = new JsonObject().put("_id", id);
      db.find(TASK_COLLECTION, query, ar -> {
        if(ar.failed()) {
          reportError(response, ar.cause());
        } else if (ar.result().size() == 0) {
          response.setStatusCode(500).setStatusMessage("could not find id: " + id);
        } else {
          response.putHeader("content-type", rc.getAcceptableContentType());
          response.end(ar.result().get(0).toString());
        }
      });
    }).produces("application/json");
  }

  private void bindCreateTask(Router router) {
    router.post("/api/task").handler(rc -> {
      HttpServerResponse response = rc.response();
      JsonObject task = rc.getBodyAsJson();

      db.insert(TASK_COLLECTION, task, arInsert -> {
        if (arInsert.failed()) {
          reportError(response, arInsert.cause());
        } else {
          JsonObject query = new JsonObject().put("_id", arInsert.result());
          JsonObject fields = new JsonObject()
					  .put("_id", true)
            .put("description", true)
						.put("done", true);

          db.findOne(TASK_COLLECTION, query, fields, arFind -> {
            if (arFind.failed()) {
              reportError(response, arFind.cause());
            } else {
              response.putHeader("content-type", rc.getAcceptableContentType());
              sendResultAndNotification(response, arFind.result(), Operation.Create);
            }
          });
        }
      });
    }).consumes("application/json")
      .produces("application/json");
  }

  private void bindUpdateTask(Router router) {
    router.put("/api/task/:id").handler(rc -> {
      String id = rc.request().getParam("id");
      HttpServerResponse response = rc.response();
      JsonObject task = rc.getBodyAsJson();
      JsonObject query = new JsonObject().put("_id", id);
      JsonObject operation = new JsonObject().put("$set", task);
      db.update(TASK_COLLECTION, query, operation, result -> {
        if (result.failed()) {
          reportError(response, result.cause());
        } else {
          response.putHeader("content-type", rc.getAcceptableContentType());
          sendResultAndNotification(response, task, Operation.Update);
        }
      });
    })
      .consumes("application/json")
      .produces("application/json");
  }

  private void bindDeleteTask(Router router) {
    router.delete("/api/task/:id").handler(rc -> {
      String id = rc.request().getParam("id");
      HttpServerResponse response = rc.response();
      response.putHeader("content-type", rc.getAcceptableContentType());

      JsonObject query = new JsonObject().put("_id", id);
      db.removeOne(TASK_COLLECTION, query, ar -> {
        if (ar.failed()) {
          reportError(response, ar.cause());
        } else {
          JsonObject result = new JsonObject()
            .put("status", "done")
            .put("id", id);
          sendResultAndNotification(response, result, Operation.Delete);
        }
      });
    })
      .produces("application/json");
  }

  private Document fixId(Document document) {
    document.put("_id", document.getObjectId("_id").toString());
    return document;
  }

  private void bindStaticFiles(Router router) {
    router.route().handler(
      StaticHandler.create()
        .setCachingEnabled(!config().containsKey("devmode"))
        .setWebRoot("static"));
  }

  private void bindBowerComponents(Router router) {
    router.route("/components/*").handler(
      StaticHandler.create()
        .setWebRoot("bower_components")
    );
  }

  @Override
  public void stop() throws Exception {
    closeHttpServer();
    closeDatabaseClient();
    logger.info("stopped");
  }

  private void closeDatabaseClient() {
    if (db != null) {
      db.close();
      db = null;
    }
  }

  private void closeHttpServer() {
    if (httpServer != null) {
      httpServer.close();
      httpServer = null;
    }
  }

  private void sendResultAndNotification(HttpServerResponse response, JsonObject result, Operation operation) {
    JsonObject notification = new JsonObject();
    notification.put("op", operation);
    notification.put("value", result);
    vertx.eventBus().publish(NOTIFICATION_TOPIC, notification);
    response.end(result.toString());
  }
}

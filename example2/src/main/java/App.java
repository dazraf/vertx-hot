import com.mongodb.rx.client.MongoClient;
import com.mongodb.rx.client.MongoClients;
import com.mongodb.rx.client.MongoCollection;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.eventbus.MessageConsumer;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.StaticHandler;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicBoolean;

import static com.mongodb.client.model.Filters.eq;

public class App extends AbstractVerticle {
  private static final Logger logger = LoggerFactory.getLogger(App.class);
  private static final String TASK_DB = "TaskApp";
  private static final java.lang.String TASK_COLLECTION = "Tasks";
  private static final String WEB_SOCKET_PORT = "/api/notifications";
  private static final String NOTIFICATION_TOPIC = "task.notification";
  private MongoClient mongoClient;
  private HttpServer httpServer;

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
    this.mongoClient = MongoClients.create();
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
      response.setChunked(true);
      response.putHeader("content-type", rc.getAcceptableContentType());
      response.write("[");
      AtomicBoolean first = new AtomicBoolean(true);
      MongoCollection<Document> collection = getDBCollection();
      collection.find().toObservable().subscribe(task -> {
          if (first.get()) {
            first.set(false);
          } else {
            response.write(",");
          }
          response.write(fixId(task).toJson());
        },
        err -> response.setStatusCode(500).setStatusMessage(err.getMessage()).end(),
        () -> response.end("]")
      );
    })
      .produces("application/json");
  }

  private void bindGetTask(Router router) {
    router.get("/api/task/:id").handler(rc -> {
      String id = rc.request().getParam("id");
      HttpServerResponse response = rc.response();
      response.putHeader("content-type", rc.getAcceptableContentType());

      getDBCollection().find(eq("_id", new ObjectId(id))).toObservable().subscribe(
        found -> response.end(fixId(found).toJson()),
        err -> response.setStatusCode(500).setStatusMessage(err.getMessage()).end(),
        () -> logger.info("get done")
      );
    }).produces("application/json");
  }

  private void bindCreateTask(Router router) {
    router.post("/api/task").handler(rc -> {
      HttpServerResponse response = rc.response();
      response.putHeader("content-type", rc.getAcceptableContentType());
      String body = rc.getBodyAsString();
      Document task = Document.parse(body);
      getDBCollection().insertOne(task).subscribe(
        success -> {
          String jsonString = fixId(task).toJson();
          sendResultAndNotification(response, jsonString, Operation.Create);
        },
        err -> response.setStatusCode(500).setStatusMessage(err.getMessage()).end());
    })
      .consumes("application/json")
      .produces("application/json");
  }

  private void bindUpdateTask(Router router) {
    router.put("/api/task/:id").handler(rc -> {
      String id = rc.request().getParam("id");
      HttpServerResponse response = rc.response();
      response.putHeader("content-type", rc.getAcceptableContentType());
      String body = rc.getBodyAsString();

      Document task = Document.parse(body);
      Bson equalId = eq("_id", new ObjectId(id));
      getDBCollection().updateOne(equalId, new Document("$set", removeId(task))).subscribe(
        success -> getDBCollection().find(equalId).toObservable().subscribe(found -> {
          sendResultAndNotification(response, fixId(found).toJson(), Operation.Update);
        }),
        err -> response.setStatusCode(500).setStatusMessage(err.getMessage()).end(),
        () -> logger.info("done update")
      );
    })
      .consumes("application/json")
      .produces("application/json");
  }

  private void bindDeleteTask(Router router) {
    router.delete("/api/task/:id").handler(rc -> {
      String id = rc.request().getParam("id");
      HttpServerResponse response = rc.response();
      response.putHeader("content-type", rc.getAcceptableContentType());

      ObjectId idObject = new ObjectId(id);
      Document idDocument = new Document("_id", idObject);
      getDBCollection().deleteOne(idDocument).subscribe(deleteResult -> {
        JsonObject result = new JsonObject();
        result
          .put("status", deleteResult.getDeletedCount() == 1 ? "done" : "failed")
          .put("id", id);
        sendResultAndNotification(response, result.toString(), Operation.Delete);
        response.end(result.toString());
      });
    })
      .produces("application/json");
  }

  private Document removeId(Document task) {
    task.remove("_id");
    return task;
  }

  private Document fixId(Document document) {
    document.put("_id", document.getObjectId("_id").toString());
    return document;
  }

  private MongoCollection<Document> getDBCollection() {
    return mongoClient.getDatabase(TASK_DB).getCollection(TASK_COLLECTION);
  }

  private void bindStaticFiles(Router router) {
    router.route().handler(
      StaticHandler.create()
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
    if (mongoClient != null) {
      mongoClient.close();
      mongoClient = null;
    }
  }

  private void closeHttpServer() {
    if (httpServer != null) {
      httpServer.close();
      httpServer = null;
    }
  }

  private void sendResultAndNotification(HttpServerResponse response, String jsonString, Operation operation) {
    JsonObject notification = new JsonObject();
    notification.put("op", operation);
    notification.put("value", new JsonObject(jsonString));
    vertx.eventBus().publish(NOTIFICATION_TOPIC, notification);
    response.end(jsonString);
  }
}

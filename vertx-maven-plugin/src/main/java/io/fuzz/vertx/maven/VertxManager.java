package io.fuzz.vertx.maven;

import com.google.common.base.Charsets;
import com.google.common.io.Resources;
import io.vertx.core.*;
import io.vertx.core.json.JsonObject;

import java.io.Closeable;
import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

public class VertxManager implements Closeable {

  {
    // We set this property to prevent Vert.x caching files loaded from the classpath on disk
    // This means if you edit the static files in your IDE then the next time they are served the new ones will
    // be served without you having to restart the main()
    // This is only useful for development - do not use this in a production server
    System.setProperty("vertx.disableFileCaching", "true");
  }

  public VertxManager() {
  }

  public void close() {
  }

  public Closeable deploy(Class<? extends Verticle> verticalClazz) {
    try {
      Vertx vertx = Vertx.vertx(new VertxOptions());

      CountDownLatch latch = new CountDownLatch(1);
      AtomicReference<AsyncResult<String>> result = new AtomicReference<>();

      JsonObject config = loadConfig();
      Verticle vertical = verticalClazz.getConstructor().newInstance();

      vertx.deployVerticle(vertical, new DeploymentOptions().setConfig(config), ar -> {
        result.set(ar);
        latch.countDown();
      });

      latch.await();

      if (result.get().succeeded()) {
        return vertx::close;
      } else {
        throw new RuntimeException("failed to deploy verticle", result.get().cause());
      }
    } catch (Throwable throwable) {
      throw new RuntimeException(throwable);
    }
  }

  private JsonObject loadConfig() throws IOException {
    return new JsonObject(Resources.toString(Resources.getResource("conf.json"), Charsets.UTF_8));
  }
}

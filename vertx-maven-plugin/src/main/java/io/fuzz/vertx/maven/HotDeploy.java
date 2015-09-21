package io.fuzz.vertx.maven;

import io.vertx.core.Verticle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rx.Observable;
import rx.Subscription;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.atomic.AtomicReference;

import static rx.Observable.just;

public class HotDeploy {
  private static final Logger logger = LoggerFactory.getLogger(HotDeploy.class);
  private final String verticalClassName;
  private final VertxManager vertxManager = new VertxManager();
  private final AtomicReference<Closeable> currentDeployment = new AtomicReference<>();
  private long startTime;

  public static void main(String[] args) throws Exception {
    run(args[0]);
  }

  public static void run(String verticleClassName) throws Exception {
    logger.info("Running HOTDEPLOY with {}", verticleClassName);
    logger.info("Current working directory: {}", Utils.getCWD());
    new HotDeploy(verticleClassName).run();
  }

  private HotDeploy(String clazzName) {
    this.verticalClassName = clazzName;
  }

  private void run() throws Exception {
    logger.info("Starting up file watchers");
    Observable<Path> fileWatch = PathWatcher.create(
      Paths.get(Utils.getCWD(), "src", "main"));

    Observable<Void> pipeline = Observable.concat(just((Path) null), fileWatch)
      .doOnNext(this::markFileDetected)
      .map(io.fuzz.vertx.maven.Compiler::compile)
      .map(MavenTargetClassLoader::create)
      .map(this::loadApp)
      .doOnNext(this::markRedeployed)
      .doOnNext(v -> System.out.println("Press Enter to Exit"));
    Subscription subscription = pipeline.subscribe();
    new BufferedReader(new InputStreamReader(System.in)).readLine();
    logger.info("stopping...");
    logger.info("unsubscribe from pipeline");
    subscription.unsubscribe();
    logger.info("close vert.x");
    vertxManager.close();
    System.exit(0);
  }

  private void markFileDetected(Path path) {
    logger.info("file change detected: {}", path);
    this.startTime = System.nanoTime();
  }

  private void markRedeployed(Void v) {
    long nanos = System.nanoTime() - startTime;
    logger.info("compiled and redeployed in {}s", nanos * 1E-9);
  }

  @SuppressWarnings("unchecked")
  private Void loadApp(ClassLoader classLoader) {
    try {
      currentDeployment.getAndUpdate(c -> {
        if (c != null) {
          try {
            c.close();
          } catch (IOException e) {
            throw new RuntimeException(e);
          }
        }
        return null;
      });
      Class<? extends Verticle> clazz = (Class<? extends Verticle>) classLoader.loadClass(verticalClassName);
      currentDeployment.compareAndSet(null, vertxManager.deploy(clazz));
    } catch (Exception e) {
      logger.error("Error in deployment: ", e);
    }
    return null;
  }
}


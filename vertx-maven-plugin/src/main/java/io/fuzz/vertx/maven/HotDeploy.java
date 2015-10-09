package io.fuzz.vertx.maven;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rx.Observable;
import rx.Subscription;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

public class HotDeploy {
  private static final Logger logger = LoggerFactory.getLogger(HotDeploy.class);
  private final String verticalClassName;
  private final VertxManager vertxManager = new VertxManager();
  private final AtomicReference<Closeable> currentDeployment = new AtomicReference<>();
  private final List<String> classPaths;
  private long startTime;

  public static void main(String[] args) throws Exception {
    run(args[0]);
  }

  public static void run(String verticleClassName) throws Exception {
    logger.info("Running HOTDEPLOY with {}", verticleClassName);
    logger.info("Current working directory: {}", Utils.getCWD());
    run(verticleClassName, Collections.emptyList());
  }

  public static void run(String verticleClassName, List<String> classPaths) throws Exception {
    logger.info("Running HOTDEPLOY with {}", verticleClassName);
    logger.info("Current working directory: {}", Utils.getCWD());
    new HotDeploy(verticleClassName, classPaths).run();
  }

  private HotDeploy(String clazzName, List<String> classPaths) throws Exception {
    this.verticalClassName = clazzName;
    this.classPaths = classPaths;
  }

  private void run() throws Exception {
    logger.info("Starting up file watchers");
    Observable<Path> fileWatch = PathWatcher.create(
      Paths.get(Utils.getCWD(), "src", "main"));

    Subscription subscription = fileWatch.subscribe(this::onFileChangeDetected, this::onError, this::onComplete);
    onFileChangeDetected(null);
    printLastMessage();
    new BufferedReader(new InputStreamReader(System.in)).readLine();
    logger.info("Unsubscribing from pipeline");
    subscription.unsubscribe();
    logger.info("close vert.x");
    vertxManager.close();
    System.exit(0);
  }

  private void printLastMessage() {
    System.out.println("Press ENTER to finish");
  }

  private void onComplete() {
    logger.info("Stopping...");
  }

  private void onError(Throwable throwable) {
    logger.error("Error during hot deploy", throwable);
  }

  private void onFileChangeDetected(Path path) {
    markFileDetected(path);
    Compiler.compile();
    loadApp(classPaths);
    markRedeployed();
  }

  private void markFileDetected(Path path) {
    logger.info("file change detected: {}", path);
    this.startTime = System.nanoTime();
  }

  private void markRedeployed() {
    long nanos = System.nanoTime() - startTime;
    logger.info("compiled and redeployed in {}s", nanos * 1E-9);
  }

  @SuppressWarnings("unchecked")
  private void loadApp(List<String> classPaths) {
    logger.info("Shutting down existing deployment");
    try {
      currentDeployment.getAndUpdate(c -> {
        if (c != null) {
          try {
            c.close();
            logger.info("Deployment shutdown");
          } catch (IOException e) {
            logger.error("Error shutting down existing deployment", e);
            throw new RuntimeException(e);
          }
        }
        return null; // clears the reference to the last deployment
      });
      logger.info("Starting deployment");
      currentDeployment.compareAndSet(null, vertxManager.deploy(verticalClassName, classPaths));
      logger.info("Deployment started");
    } catch (Exception e) {
      logger.error("Error in deployment: ", e);
    }
  }
}


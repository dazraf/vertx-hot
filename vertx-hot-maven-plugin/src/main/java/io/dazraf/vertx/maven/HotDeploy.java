package io.dazraf.vertx.maven;

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
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

public class HotDeploy {
  private static final Logger logger = LoggerFactory.getLogger(HotDeploy.class);
  private final String verticalClassName;
  private final VertxManager vertxManager = new VertxManager();
  private final AtomicReference<Closeable> currentDeployment = new AtomicReference<>();
  private final List<String> classPaths;
  private final Optional<String> config;
  private final List<String> sourcePaths;
  private long startTime;

  public static void run(String verticleClassName, List<String> classPaths, Optional<String> config, List<String> sourcePaths
  ) throws Exception {
    logger.info("Running HOTDEPLOY with {}", verticleClassName);
    logger.info("Current working directory: {}", Utils.getCWD());
    new HotDeploy(verticleClassName, classPaths, config, sourcePaths).run();
  }

  private HotDeploy(String clazzName, List<String> classPaths, Optional<String> config, List<String> sourcePaths) throws Exception {
    this.verticalClassName = clazzName;
    this.classPaths = classPaths;
    this.config = config;
    this.sourcePaths = sourcePaths;
  }

  private void run() throws Exception {
    logger.info("Starting up file watchers");
    Observable<Path> fileWatch = Observable.merge(
      sourcePaths.stream().map(path -> {
        try {
          return PathWatcher.create(Paths.get(path));
        } catch (Exception e) {
          logger.error("error in creating path watcher for path: " + path, e);
          throw new RuntimeException(e);
        }
      }).collect(Collectors.toList()));

    Subscription subscription = fileWatch.buffer(1, TimeUnit.SECONDS).subscribe(
      this::onFileChangeDetected,
      this::onError,
      this::onComplete);
    onFileChangeDetected(null);
    printLastMessage();
    new BufferedReader(new InputStreamReader(System.in)).readLine();
    logger.info("shutting down ...");
    subscription.unsubscribe();
    vertxManager.close();
    logger.info("done");
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

  private void onFileChangeDetected(List<Path> paths) {
    if (paths == null || paths.size() > 0) {
      markFileDetected(Optional.ofNullable(paths));
      Compiler.compile();
      loadApp(classPaths);
      markRedeployed();
    }
  }

  private void markFileDetected(Optional<List<Path>> paths) {
    if (paths.isPresent()) {
      logger.info("file change detected:");
      paths.get().stream().forEach(path -> logger.info(path.toString()));
    }
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
      currentDeployment.compareAndSet(null, vertxManager.deploy(verticalClassName, classPaths, config));
      logger.info("Deployment started");
    } catch (Exception e) {
      logger.error("Error in deployment: ", e);
    }
  }
}


package io.dazraf.vertx.maven;

import io.vertx.core.eventbus.MessageProducer;
import io.vertx.core.json.JsonObject;
import org.apache.maven.project.MavenProject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rx.Observable;
import rx.Subscription;

import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

public class HotDeploy {
  private final MavenProject project;

  public enum DeployStatus {
    COMPILING,
    DEPLOYING,
    DEPLOYED,
    FAILED,
    STOPPED
  }

  private static final Logger logger = LoggerFactory.getLogger(HotDeploy.class);
  private final String verticalClassName;
  private final VerticleDeployer verticleDeployer;
  private final MessageProducer<JsonObject> statusProducer;
  private final AtomicReference<Closeable> currentDeployment = new AtomicReference<>();
  private final Optional<String> config;
  private final List<String> sourcePaths;
  private final Compiler compiler = new Compiler();
  private long startTime;

  public static void run(MavenProject project,
                         List<String> watchedPaths,
                         String verticleClassName,
                         Optional<String> config,
                         boolean liveHttpReload
  ) throws Exception {
    logger.info("Running HOTDEPLOY with {}", verticleClassName);
    new HotDeploy(project, watchedPaths, verticleClassName, config, liveHttpReload).run();
  }

  private HotDeploy(MavenProject project,
                    List<String> sourcePaths,
                    String verticleClassName,
                    Optional<String> config,
                    boolean liveHttpReload) {
    this.verticleDeployer = new VerticleDeployer(liveHttpReload);
    this.statusProducer = verticleDeployer.createEventProducer();
    this.project = project;
    this.verticalClassName = verticleClassName;
    this.config = config;
    this.sourcePaths = sourcePaths;
  }

  private void run() throws Exception {
    logger.info("Starting up file watchers");
    Observable<Path> fileWatch = getFileWatchObservable();
    Subscription subscription = fileWatch.buffer(1, TimeUnit.SECONDS).subscribe(
      this::onFileChangeDetected,
      this::onError,
      this::onComplete);

    compileAndDeploy();
    waitForNewLine();

    logger.info("shutting down ...");
    sendStatus(DeployStatus.STOPPED);
    subscription.unsubscribe();
    verticleDeployer.close();
    logger.info("done");
  }

  private Observable<Path> getFileWatchObservable() {
    return Observable.merge(
      sourcePaths.stream().map(path -> {
        try {
          return PathWatcher.create(Paths.get(path));
        } catch (Exception e) {
          logger.error("Error in creating path watcher for path: " + path, e);
          throw new RuntimeException(e);
        }
      }).collect(Collectors.toList()));
  }

  private void waitForNewLine() throws IOException {
    new BufferedReader(new InputStreamReader(System.in)).readLine();
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
      if (paths != null) {
        logger.info("file change detected:");
        paths.stream().forEach(path -> logger.info(path.toString()));
      }
      compileAndDeploy();
    }
  }

  private void compileAndDeploy() {
    markFileDetected();
    logger.info("Compiling...");
    sendStatus(DeployStatus.COMPILING);

    try {
      loadApp(compiler.compile(project));
    } catch (Exception e) {
      logger.error("error", e);
      sendStatus(e);
    }

    markRedeployed();
    printLastMessage();
  }

  private void markFileDetected() {
    this.startTime = System.nanoTime();
  }

  private void markRedeployed() {
    long nanos = System.nanoTime() - startTime;
    String status = currentDeployment.get() != null ? "Compiled and redeployed" : "Deployment failed";
    logger.info("{} in {}s", status, String.format("%1.3f", nanos * 1E-9));
  }

  private void loadApp(List<String> classPaths) {
    // atomic
    currentDeployment.getAndUpdate(c -> {
      sendStatus(DeployStatus.DEPLOYING);
      // if we have a deployment, shut it down
      if (c != null) {
        try {
          logger.info("Shutting down existing deployment");
          c.close();
          logger.info("Deployment shutdown");
        } catch (IOException e) {
          logger.error("Error shutting down existing deployment", e);
          throw new RuntimeException(e);
        }
      }

      // deploy
      try {
        logger.info("Starting deployment");
        Closeable closeable = verticleDeployer.deploy(verticalClassName, classPaths, config);
        sendStatus(DeployStatus.DEPLOYED);
        return closeable;
      } catch (Throwable e) {
        sendStatus(e);
        logger.error("Error in deployment", e);
        return null;
      }
    });
  }

  private void sendStatus(DeployStatus deployStatus) {
    statusProducer.write(
      new JsonObject()
        .put("status", deployStatus.toString()));
  }

  private void sendStatus(Throwable e) {
    statusProducer.write(
      new JsonObject()
        .put("status", DeployStatus.FAILED.toString())
        .put("cause", e.getMessage()));
  }
}

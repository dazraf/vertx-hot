package io.dazraf.vertx.maven;

import io.dazraf.vertx.maven.compiler.CompileResult;
import io.dazraf.vertx.maven.compiler.Compiler;
import io.dazraf.vertx.maven.deployer.VerticleDeployer;
import io.dazraf.vertx.maven.filewatcher.PathWatcher;
import io.vertx.core.eventbus.MessageProducer;
import io.vertx.core.json.JsonObject;
import org.apache.maven.model.Resource;
import org.apache.maven.project.MavenProject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rx.Observable;
import rx.Subscription;

import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.function.Function.identity;
import static java.util.stream.Stream.empty;
import static java.util.stream.Stream.of;

public class HotDeploy {
  public enum DeployStatus {
    COMPILING,
    DEPLOYING,
    DEPLOYED,
    FAILED,
    STOPPED
  }

  private static final Logger logger = LoggerFactory.getLogger(HotDeploy.class);
  private final HotDeployParameters parameters;
  private final Compiler compiler = new Compiler();
  private final VerticleDeployer verticleDeployer;
  private final MessageProducer<JsonObject> statusProducer;
  private final AtomicReference<Closeable> currentDeployment = new AtomicReference<>();

  public static void run(HotDeployParameters parameters) throws Exception {
    logger.info("Running HOTDEPLOY with {}", parameters.toString());
    new HotDeploy(parameters).run();
  }

  private HotDeploy(HotDeployParameters parameters) {
    this.parameters = parameters;
    this.verticleDeployer = new VerticleDeployer(parameters.isLiveHttpReload(), parameters.getNotificationPort());
    this.statusProducer = verticleDeployer.createEventProducer();
  }

  private void run() throws Exception {
    logger.info("Starting up file watchers");

    Subscription compilableFileSubscription = watchCompilableFileEvents()
      .buffer(1, TimeUnit.SECONDS)
      .subscribe(
      this::onCompilableFileEvent,
      this::onError,
      this::onComplete);

    Subscription refreshableFileSubscription = watchRefreshableFileEvents()
      .buffer(1, TimeUnit.SECONDS)
      .subscribe(this::onRefreshableFileEvent, this::onError);

    compileAndDeploy();

    waitForNewLine();

    logger.info("shutting down ...");
    sendStatus(DeployStatus.STOPPED);
    compilableFileSubscription.unsubscribe();
    refreshableFileSubscription.unsubscribe();
    verticleDeployer.close();
    logger.info("done");
  }


  private Observable<Path> watchCompilableFileEvents() {
    return Observable.merge(
      getCompilableFilePaths()
        .map(this::createWatch)
        .collect(Collectors.toList()));
  }

  private Observable<Path> watchRefreshableFileEvents() {
    return Observable.merge(
      getWatchableResources()
        .map(this::createWatch)
        .collect(Collectors.toList()));
  }

  private Observable<Path> createWatch(String path) {
    try {
      return PathWatcher.create(Paths.get(path));
    } catch (Exception e) {
      logger.error("Error in creating path watcher for path: " + path, e);
      throw new RuntimeException(e);
    }
  }

  private Stream<String> getCompilableFilePaths() {
    return of(
      project().getCompileSourceRoots().stream(), // set of compile sources
      getBuildableResources(), // the resources
      of(project().getFile().getAbsolutePath()) // the pom file itself
    )
      .flatMap(identity());
  }

  private Stream<String> getBuildableResources() {
    if (parameters.isBuildResources()) {
      return project().getResources().stream().map(Resource::getDirectory);
    } else {
      return empty();
    }
  }

  private Stream<String> getWatchableResources() {
    if (parameters.isLiveHttpReload() && !parameters.isBuildResources()) {
      return project().getResources().stream().map(Resource::getDirectory);
    } else {
      return empty();
    }
  }

  private MavenProject project() {
    return parameters.getProject();
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

  private void onCompilableFileEvent(List<Path> paths) {
    if (paths == null || paths.size() > 0) {
      if (paths != null) {
        logger.info("file change detected:");
        paths.stream().forEach(path -> logger.info(path.toString()));
      }
      compileAndDeploy();
    }
  }

  private void onRefreshableFileEvent(List<Path> paths) {
    if (paths != null && paths.size() > 0) {
      if (pathsContainConfig(paths)) {
        compileAndDeploy();
      } else {
        sendStatus(DeployStatus.DEPLOYED);
      }
    }
  }

  private boolean pathsContainConfig(List<Path> paths) {
    return paths.stream().anyMatch(f -> parameters.getConfigFileName().map(f::endsWith).orElse(false));
  }

  private void compileAndDeploy() {
    long startTime = markFileDetected();
    logger.info("Compiling...");
    sendStatus(DeployStatus.COMPILING);

    try {
      loadApp(compiler.compile(project()));
    } catch (Exception e) {
      logger.error("error", e);
      sendStatus(e);
    }

    markRedeployed(startTime);
    printLastMessage();
  }

  private long markFileDetected() {
    return System.nanoTime();
  }

  private void markRedeployed(long startTime) {
    long nanos = System.nanoTime() - startTime;
    String status = currentDeployment.get() != null ? "Compiled and redeployed" : "Deployment failed";
    logger.info("{} in {}s", status, String.format("%1.3f", nanos * 1E-9));
  }

  private void loadApp(CompileResult compileResult) {
    // atomic
    currentDeployment.getAndUpdate(existingVerticle -> {
      sendStatus(DeployStatus.DEPLOYING);
      closeExistingVerticle(existingVerticle);
      return deployNewVerticle(compileResult);
    });
  }

  private Closeable deployNewVerticle(CompileResult compileResult) {
    // deploy
    try {
      logger.info("Starting deployment");
      Closeable closeable = verticleDeployer.deploy(parameters.getVerticleClassName(), compileResult.getClassPath(), parameters.getConfigFileName());
      sendStatus(DeployStatus.DEPLOYED);
      return closeable;
    } catch (Throwable e) {
      sendStatus(e);
      logger.error("Error in deployment", e);
      return null;
    }
  }

  private void closeExistingVerticle(Closeable existingVerticle) {
    // if we have a deployment, shut it down
    if (existingVerticle != null) {
      try {
        logger.info("Shutting down existing deployment");
        existingVerticle.close();
        logger.info("Deployment shutdown");
      } catch (IOException e) {
        logger.error("Error shutting down existing deployment", e);
        throw new RuntimeException(e);
      }
    }
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

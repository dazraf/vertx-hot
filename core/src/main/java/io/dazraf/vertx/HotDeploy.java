package io.dazraf.vertx;

import io.dazraf.vertx.compiler.CompileResult;
import io.dazraf.vertx.compiler.Compiler;
import io.dazraf.vertx.compiler.CompilerException;
import io.dazraf.vertx.deployer.VerticleDeployer;
import io.dazraf.vertx.filewatcher.PathWatcher;
import io.dazraf.vertx.paths.PathResolver;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rx.Observable;
import rx.Subscription;
import rx.functions.Action1;
import rx.subjects.PublishSubject;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static java.util.stream.Collectors.toList;

public class HotDeploy {

  public enum DeployStatus {
    COMPILING,
    DEPLOYING,
    DEPLOYED,
    FAILED,
    STOPPED
  }

  private static final Logger logger = LoggerFactory.getLogger(HotDeploy.class);
  private final Compiler compiler;
  private final VerticleDeployer verticleDeployer;
  private final PathResolver pathResolver;
  private final Awaitable awaitable;
  private final PublishSubject<JsonObject> statusSubject;
  private final AtomicReference<Closeable> currentDeployment = new AtomicReference<>();
  private final AtomicReference<CompileResult> lastCompileResult = new AtomicReference<>();

  public HotDeploy(Compiler compiler, VerticleDeployer verticleDeployer, PathResolver pathResolver) {
    this(compiler, verticleDeployer, pathResolver, createWaitForNewLine());
  }

  public HotDeploy(Compiler compiler, VerticleDeployer verticleDeployer, PathResolver pathResolver,
            Awaitable awaitable) {
    this.statusSubject = PublishSubject.create();
    this.compiler = compiler;
    this.verticleDeployer = verticleDeployer;
    this.pathResolver = pathResolver;
    this.awaitable = awaitable;
    subscribeToStatusUpdates(verticleDeployer.createStatusConsumer());
  }

  private void subscribeToStatusUpdates(Action1<JsonObject> observer) {
    statusSubject.subscribe(observer);
  }

  public void run() throws Exception {
    logger.info("Starting up file watchers");

    Subscription compilableFileSubscription = watchCompilableFileEvents()
      .buffer(1, TimeUnit.SECONDS)
      .subscribe(
      this::onCompilableFileEvent,
      this::onError,
      this::onComplete);

    Subscription redeployableFileEvent = watchRedeployableFileEvents()
      .buffer(1, TimeUnit.SECONDS)
      .subscribe(this::onRedeployableFileEvent, this::onError);

    Subscription refreshableFileSubscription = watchRefreshableFileEvents()
      .buffer(1, TimeUnit.SECONDS)
      .subscribe(this::onRefreshableFileEvent, this::onError);

    compile();

    awaitable.await();

    logger.info("shutting down ...");
    sendStatus(DeployStatus.STOPPED);
    compilableFileSubscription.unsubscribe();
    refreshableFileSubscription.unsubscribe();
    redeployableFileEvent.unsubscribe();
    verticleDeployer.close();
    logger.info("done");
  }

  // --- File watching functions ---
  private Observable<Path> watchCompilableFileEvents() {
    logger.info("compilable paths");
    return Observable.merge(
      pathResolver.pathsThatRequireCompile().stream()
        .peek(p -> logger.info(p.toString()))
        .map(this::createWatch)
        .collect(toList()));
  }

  private Observable<Path> watchRedeployableFileEvents() {
    logger.info("redeployable paths");
    return Observable.merge(
      pathResolver.pathsThatRequireRedeploy().stream()
        .peek(p -> logger.info(p.toString()))
        .map(this::createWatch)
        .collect(toList()));
  }


  private Observable<Path> watchRefreshableFileEvents() {
    logger.info("refreshable paths");
    return Observable.merge(
      pathResolver.pathsThatRequireBrowserRefresh().stream()
        .peek(p -> logger.info(p.toString()))
        .map(this::createWatch)
        .collect(toList()));
  }

  private Observable<Path> createWatch(Path path) {
    try {
      return PathWatcher.create(path);
    } catch (Exception e) {
      logger.error("Error in creating path watcher for path: " + path, e);
      throw new RuntimeException(e);
    }
  }

  // --- File event handlers ---

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
      compile(); // we compile for the edge case where paths == null to trigger the first build
      printLastMessage();
    }
  }

  private void onRedeployableFileEvent(List<Path> paths) {
    if (paths != null && paths.size() > 0) {
      deploy();
      printLastMessage();
    }
  }

  private void onRefreshableFileEvent(List<Path> paths) {
    if (paths != null && paths.size() > 0) {
      refreshBrowser();
    }
  }

  // --- Core Functions -- //

  private void compile() {
    long startTime = markFileDetectedAction();
    logger.info("Compiling...");
    sendStatus(DeployStatus.COMPILING);

    try {
      lastCompileResult.set(compiler.compile());
      logger.info("Done");
      markActionCompleted(startTime, "Compiled");
      deploy();
    } catch(CompilerException e) {
      sendStatus(e);
    } catch(Exception e) {
      logger.error("error", e);
      sendStatus(e);
    }
  }

  private void deploy() {
    long startTime = markFileDetectedAction();
    logger.info("Redeploying...");
    sendStatus(DeployStatus.DEPLOYING);

    try {
      currentDeployment.getAndUpdate(existingVerticle -> {
        sendStatus(DeployStatus.DEPLOYING);
        closeExistingVerticle(existingVerticle);
        return deployNewVerticle(lastCompileResult.get());
      });

      markActionCompleted(startTime, currentDeployment.get() != null ? "Deployed" : "Deployment failed");
    } catch(Exception e) {
      logger.error("error", e);
      sendStatus(e);
    }
  }

  private void refreshBrowser() {
    sendStatus(DeployStatus.DEPLOYED);
  }

  private long markFileDetectedAction() {
    return System.nanoTime();
  }

  private void markActionCompleted(long startTime, String actionMsg) {
    long nanos = System.nanoTime() - startTime;
    logger.info("{} in {}s", actionMsg, String.format("%1.3f", nanos * 1E-9));
  }

  private Closeable deployNewVerticle(CompileResult compileResult) {
    // deploy
    try {
      logger.info("Starting deployment using classspath {}", compileResult.getClassPath());
      Closeable closeable = verticleDeployer.deploy(compileResult.getClassPath());
      refreshBrowser();
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
    statusSubject.onNext(
      new JsonObject()
        .put("status", deployStatus.toString()));
  }

  private void sendStatus(Throwable e) {
    JsonObject status = new JsonObject()
      .put("status", DeployStatus.FAILED.toString())
      .put("cause", e.getMessage());

    if (e instanceof CompilerException) {
      CompilerException ce = (CompilerException)e;
      JsonArray messages = new JsonArray(ce.getMessages().stream().collect(toList()));
      status.put("messages", messages);
    }

    statusSubject.onNext(status);
  }

  private static Awaitable createWaitForNewLine() {
    return () -> {
      try {
        new BufferedReader(new InputStreamReader(System.in)).readLine();
      } catch (IOException e) {
        logger.error("whilst awaiting new line", e);
      }
    };
  }

  private void printLastMessage() {
    System.out.println("Press ENTER to finish");
  }


  @FunctionalInterface
  interface Awaitable {
    void await();
  }
}

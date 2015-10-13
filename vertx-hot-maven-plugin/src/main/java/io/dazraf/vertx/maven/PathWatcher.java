package io.dazraf.vertx.maven;

import com.darylteo.nio.DirectoryChangedSubscriber;
import com.darylteo.nio.DirectoryWatcher;
import com.darylteo.nio.ThreadPoolDirectoryWatchService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rx.Observable;
import rx.subjects.PublishSubject;

import java.nio.file.Path;

public class PathWatcher {
  private final static Logger logger = LoggerFactory.getLogger(PathWatcher.class);

  public static Observable<Path> create(Path path) throws Exception {
    ThreadPoolDirectoryWatchService factory = new ThreadPoolDirectoryWatchService();

    logger.info("Watching: {}", path);
    DirectoryWatcher directoryWatcher = factory.newWatcher(path);

    PublishSubject<Path> subject = PublishSubject.create();
    DirectoryChangedSubscriber directoryChangedSubscriber = new DirectoryChangedSubscriber() {
      @Override
      public void directoryChanged(DirectoryWatcher directoryWatcher, Path changedPath) {
        subject.onNext(changedPath);
      }
    };
    directoryWatcher.subscribe(directoryChangedSubscriber);
    subject.doOnUnsubscribe(() -> {
      logger.info("... unsubscribing from directory watch");
      directoryWatcher.unsubscribe(directoryChangedSubscriber);
      try {
        factory.close();
      } catch (Exception e) {
        logger.error("error in shutting down directory watch factory", e);
      }
    });
    return subject;
  }
}

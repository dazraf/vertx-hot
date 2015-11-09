package io.dazraf.vertx.maven.filewatcher;

import com.darylteo.nio.DirectoryChangedSubscriber;
import com.darylteo.nio.DirectoryWatcher;
import com.darylteo.nio.ThreadPoolDirectoryWatchService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rx.Observable;
import rx.functions.Action0;
import rx.subjects.PublishSubject;

import java.io.IOException;
import java.nio.file.Path;
import java.util.function.Consumer;

public class PathWatcher {
  private final static Logger logger = LoggerFactory.getLogger(PathWatcher.class);

  public static Observable<Path> create(final Path path) throws Exception {
    logger.info("Watching: {}", path);

    PublishSubject<Path> subject = PublishSubject.create();

    if (path.toFile().isDirectory()) {
      subject.doOnSubscribe(createDirectoryWatcher(path, subject::onNext));
    } else {
      subject.doOnSubscribe(createDirectoryWatcher(path.getParent(), changedPath -> {
        if (path.getParent().resolve(changedPath).equals(path)) {
          subject.onNext(changedPath);
        }
      }));
    }
    return subject;
  }

  private static Action0 createDirectoryWatcher(Path path, Consumer<Path> callback) throws IOException {
    ThreadPoolDirectoryWatchService factory = new ThreadPoolDirectoryWatchService();
    DirectoryWatcher directoryWatcher = factory.newWatcher(path);
    DirectoryChangedSubscriber directoryChangedSubscriber = new DirectoryChangedSubscriber() {
      @Override
      public void directoryChanged(DirectoryWatcher directoryWatcher, Path changedPath) {
        callback.accept(changedPath);
      }
    };

    directoryWatcher.subscribe(directoryChangedSubscriber);
    return () -> {
      logger.info("... unsubscribing from directory watch");
      directoryWatcher.unsubscribe(directoryChangedSubscriber);
      try {
        factory.close();
      } catch (Exception e) {
        logger.error("error in shutting down directory watch factory", e);
      }
    };
  }
}

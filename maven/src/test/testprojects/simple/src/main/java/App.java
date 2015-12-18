import io.vertx.core.AbstractVerticle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class App extends AbstractVerticle {
  private static final Logger LOGGER = LoggerFactory.getLogger(App.class);

  @Override
  public void start() throws Exception {
    LOGGER.info("starting ... ");
    int port = config().getInteger("port", 8080);
    vertx.createHttpServer().requestHandler(req -> req.response().end("OK")).listen(port);
    LOGGER.info("started: http://localhost:{}", port);
  }
}
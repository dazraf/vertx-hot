package io.vertx.core.impl;

import io.dazraf.vertx.maven.HttpServerRequestWrapper;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.impl.HttpServerImpl;
import io.vertx.ext.web.Router;

import java.io.*;

public class VertxWrapper extends VertxImpl {
  private static final String BASE_API_PATH = "\\/__vertx_hot\\/(.*)";
  private final ClassLoader pluginClassloader = Thread.currentThread().getContextClassLoader();

  public VertxWrapper(VertxOptions options) {
    super(options);
  }

  public VertxWrapper(VertxOptions options, Handler<AsyncResult<Vertx>> resultHandler) {
    super(options, resultHandler);
  }

  //
  /**
   * the http server interceptor has the following responsibilities
   * 1. any request to {@link BASE_API_PATH} is routed to a file server for late loaded artifacts
   * 2. all other requests are dispatched accordingly to the requestHandler of the application
   *    but passing through a {@link HttpServerRequestWrapper} to inject dependencies on late-loaded artifacts
   */
  private class HttpServerInterceptor extends HttpServerImpl {

    public HttpServerInterceptor(VertxInternal vertx, HttpServerOptions options) {
      super(vertx, options);
    }

    @Override
    public synchronized HttpServer requestHandler(Handler<HttpServerRequest> handler) {
      Router router = Router.router(VertxWrapper.this);

      router.getWithRegex(BASE_API_PATH).handler(rc -> {
          String filename = rc.request().getParam("param0");
          try {
            try (InputStream inputStream = pluginClassloader.getResourceAsStream(filename)) {
              byte[] b = readBytesFromStream(inputStream);
              rc.response().end(Buffer.buffer(b));
            }
          } catch (Exception e) {
            rc.response().end("Failed");
          }
        }
      );
      router.route("/*").handler(rc -> {
        HttpServerRequest sr2 = new HttpServerRequestWrapper(rc.request(), VertxWrapper.this::resolveFile);
        handler.handle(sr2);
      });
      super.requestHandler(router::accept);
      return this;
    }
  }

  private byte[] readBytesFromStream(InputStream in) throws IOException {
    try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
      byte[] buffer = new byte[1024];
      while (true) {
        int r = in.read(buffer);
        if (r == -1) break;
        out.write(buffer, 0, r);
      }
      return out.toByteArray();
    }
  }
  @Override
  public HttpServer createHttpServer() {
    return new HttpServerInterceptor(this, new HttpServerOptions());
  }

  @Override
  public HttpServer createHttpServer(HttpServerOptions serverOptions) {
    return new HttpServerInterceptor(this, serverOptions);
  }

}

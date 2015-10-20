package io.vertx.core.impl;

import io.dazraf.vertx.maven.HttpServerRequestWrapper;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.impl.HttpServerImpl;

public class VertxWrapper extends VertxImpl {

  public VertxWrapper(VertxOptions options) {
    super(options);
  }

  public VertxWrapper(VertxOptions options, Handler<AsyncResult<Vertx>> resultHandler) {
    super(options, resultHandler);
  }

  private class HttpServerInterceptor extends HttpServerImpl {
    public HttpServerInterceptor(VertxInternal vertx, HttpServerOptions options) {
      super(vertx, options);
    }

    @Override
    public synchronized HttpServer requestHandler(Handler<HttpServerRequest> handler) {
      super.requestHandler(sr -> {
        HttpServerRequest sr2 = new HttpServerRequestWrapper(sr, VertxWrapper.this::resolveFile);
        handler.handle(sr2);
      });
      return this;
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

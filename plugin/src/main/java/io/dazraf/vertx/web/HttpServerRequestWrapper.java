package io.dazraf.vertx.web;

import io.vertx.core.Handler;
import io.vertx.core.MultiMap;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.*;
import io.vertx.core.net.NetSocket;
import io.vertx.core.net.SocketAddress;

import javax.net.ssl.SSLPeerUnverifiedException;
import javax.security.cert.X509Certificate;
import java.io.File;
import java.util.function.Function;

public class HttpServerRequestWrapper implements HttpServerRequest {
  private final HttpServerRequest wrapped;
  private final HttpServerResponse response;

  public HttpServerRequestWrapper(HttpServerRequest wrapped, Function<String, File> fileResolver) {
    this.wrapped = wrapped;
    this.response = new InjectingHttpServerResponse(wrapped.response(), fileResolver);
  }

  @Override
  public HttpServerRequest exceptionHandler(Handler<Throwable> handler) {
    wrapped.exceptionHandler(handler);
    return this;
  }

  @Override
  public HttpServerRequest handler(Handler<Buffer> handler) {
    wrapped.handler(handler);
    return this;
  }

  @Override
  public HttpServerRequest pause() {
    wrapped.pause();
    return this;
  }

  @Override
  public HttpServerRequest resume() {
    wrapped.resume();
    return this;
  }

  @Override
  public HttpServerRequest endHandler(Handler<Void> endHandler) {
    wrapped.endHandler(endHandler);
    return this;
  }

  @Override
  public HttpVersion version() {
    return wrapped.version();
  }

  @Override
  public HttpMethod method() {
    return wrapped.method();
  }

  @Override
  public String uri() {
    return wrapped.uri();
  }

  @Override
  public String path() {
    return wrapped.path();
  }

  @Override
  public String query() {
    return wrapped.query();
  }

  @Override
  public HttpServerResponse response() {
    return response;
  }

  @Override
  public MultiMap headers() {
    return wrapped.headers();
  }

  @Override
  public String getHeader(String headerName) {
    return wrapped.getHeader(headerName);
  }

  @Override
  public String getHeader(CharSequence headerName) {
    return wrapped.getHeader(headerName);
  }

  @Override
  public MultiMap params() {
    return wrapped.params();
  }

  @Override
  public String getParam(String paramName) {
    return wrapped.getParam(paramName);
  }

  @Override
  public SocketAddress remoteAddress() {
    return wrapped.remoteAddress();
  }

  @Override
  public SocketAddress localAddress() {
    return wrapped.localAddress();
  }

  @Override
  public X509Certificate[] peerCertificateChain() throws SSLPeerUnverifiedException {
    return wrapped.peerCertificateChain();
  }

  @Override
  public String absoluteURI() {
    return wrapped.absoluteURI();
  }

  @Override
  public HttpServerRequest bodyHandler(Handler<Buffer> bodyHandler) {
    return wrapped.bodyHandler(bodyHandler);
  }

  @Override

  public NetSocket netSocket() {
    return wrapped.netSocket();
  }

  @Override
  public HttpServerRequest setExpectMultipart(boolean expect) {
    return wrapped.setExpectMultipart(expect);
  }

  @Override
  public boolean isExpectMultipart() {
    return wrapped.isExpectMultipart();
  }

  @Override
  public HttpServerRequest uploadHandler(Handler<HttpServerFileUpload> uploadHandler) {
    return wrapped.uploadHandler(uploadHandler);
  }

  @Override
  public MultiMap formAttributes() {
    return wrapped.formAttributes();
  }

  @Override
  public String getFormAttribute(String attributeName) {
    return wrapped.getFormAttribute(attributeName);
  }

  @Override
  public ServerWebSocket upgrade() {
    return wrapped.upgrade();
  }

  @Override
  public boolean isEnded() {
    return  wrapped.isEnded();
  }
}

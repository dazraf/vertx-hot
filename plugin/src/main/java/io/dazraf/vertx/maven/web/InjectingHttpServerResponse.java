package io.dazraf.vertx.maven.web;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.MultiMap;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.http.impl.MimeMapping;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.StreamSupport;

import static java.util.Optional.*;

public class InjectingHttpServerResponse implements HttpServerResponse {
  private final HttpServerResponse wrapped;
  private final String injectedScript = "<script src='/__vertx_hot/scripts/connection.js'></script>";
  private boolean htmlDetected = false;
  private final Function<String, File> fileResolver;

  public InjectingHttpServerResponse(HttpServerResponse response, Function<String, File> fileResolver) {
    this.wrapped = response;
    this.fileResolver = fileResolver;
    this.wrapped.setChunked(true);
  }

  @Override
  public HttpServerResponse exceptionHandler(Handler<Throwable> handler) {
    wrapped.exceptionHandler(handler);
    return this;
  }

  @Override
  public HttpServerResponse write(Buffer data) {
    detectHtml(data);
    wrapped.write(data);
    return this;
  }

  @Override
  public HttpServerResponse setWriteQueueMaxSize(int maxSize) {
    wrapped.setWriteQueueMaxSize(maxSize);
    return this;
  }

  @Override
  public boolean writeQueueFull() {
    return wrapped.writeQueueFull();
  }

  @Override
  public HttpServerResponse drainHandler(Handler<Void> handler) {
    wrapped.drainHandler(handler);
    return this;
  }

  @Override
  public int getStatusCode() {
    return wrapped.getStatusCode();
  }

  @Override
  public HttpServerResponse setStatusCode(int statusCode) {
    wrapped.setStatusCode(statusCode);
    return this;
  }

  @Override
  public String getStatusMessage() {
    return wrapped.getStatusMessage();
  }

  @Override
  public HttpServerResponse setStatusMessage(String statusMessage) {
    wrapped.setStatusMessage(statusMessage);
    return this;
  }

  @Override
  public HttpServerResponse setChunked(boolean chunked) {
    // wrapped.setChunked(true);
    return this;
  }

  @Override
  public boolean isChunked() {
    return wrapped.isChunked();
  }

  @Override
  public MultiMap headers() {
    return wrapped.headers();
  }

  @Override
  public HttpServerResponse putHeader(String name, String value) {
    htmlDetected = htmlDetected || (name.equalsIgnoreCase("content-type") && value.contains("text/html"));
    wrapped.putHeader(name, value);
    return this;
  }

  @Override
  public HttpServerResponse putHeader(CharSequence name, CharSequence value) {
    wrapped.putHeader(name, value);
    return this;
  }

  @Override
  public HttpServerResponse putHeader(String name, Iterable<String> values) {
    htmlDetected = htmlDetected || (name.equalsIgnoreCase("content-type") &&
      StreamSupport.stream(values.spliterator(), false)
        .filter(s -> s.equalsIgnoreCase("text/html")).findFirst().isPresent());
    wrapped.putHeader(name, values);
    return this;
  }

  @Override
  public HttpServerResponse putHeader(CharSequence name, Iterable<CharSequence> values) {
    wrapped.putHeader(name, values);
    return this;
  }

  @Override
  public MultiMap trailers() {
    return wrapped.trailers();
  }

  @Override
  public HttpServerResponse putTrailer(String name, String value) {
    wrapped.putTrailer(name, value);
    return this;
  }

  @Override
  public HttpServerResponse putTrailer(CharSequence name, CharSequence value) {
    wrapped.putTrailer(name, value);
    return this;
  }

  @Override
  public HttpServerResponse putTrailer(String name, Iterable<String> values) {
    wrapped.putTrailer(name, values);
    return this;
  }

  @Override
  public HttpServerResponse putTrailer(CharSequence name, Iterable<CharSequence> value) {
    wrapped.putTrailer(name, value);
    return this;
  }

  @Override
  public HttpServerResponse closeHandler(Handler<Void> handler) {
    wrapped.closeHandler(handler);
    return this;
  }

  @Override
  public HttpServerResponse write(String chunk, String enc) {
    htmlDetected = htmlDetected || chunk.toLowerCase().contains("<html");
    wrapped.write(chunk, enc);
    return this;
  }

  @Override
  public HttpServerResponse write(String chunk) {
    htmlDetected = htmlDetected || chunk.toLowerCase().contains("<html");
    wrapped.write(chunk);
    return this;
  }

  @Override
  public HttpServerResponse writeContinue() {
    wrapped.writeContinue();
    return this;
  }

  @Override
  public void end(String chunk) {
    htmlDetected = htmlDetected || chunk.toLowerCase().contains("<html");
    if (htmlDetected) {
      wrapped.end(appendClose(chunk));
    } else {
      wrapped.end(chunk);
    }
  }

  @Override
  public void end(String chunk, String enc) {
    htmlDetected = htmlDetected || chunk.toLowerCase().contains("<html");
    if (htmlDetected) {
      wrapped.end(appendClose(chunk), enc);
    } else {
      wrapped.end(chunk, enc);
    }
  }

  @Override
  public void end(Buffer chunk) {
    if (detectHtml(chunk)) {
      Buffer newBuffer = chunk.copy().appendString(injectedScript);
      wrapped.end(newBuffer);
    } else {
      wrapped.end(chunk);
    }
  }

  @Override
  public void end() {
    if (htmlDetected) {
      wrapped.end(injectedScript);
    } else {
      wrapped.end();
    }
  }

  @Override
  public HttpServerResponse sendFile(String filename, long offset, long length) {
    return rewriteStaticHTMLFile(filename, offset, length)
      .map(buffer -> sendBufferForFile(filename, buffer)).orElseGet(() -> this);
  }


  @Override
  public HttpServerResponse sendFile(String filename, long offset, long length, Handler<AsyncResult<Void>> resultHandler) {
    return rewriteStaticHTMLFile(filename, offset, length)
      .map(buffer -> {
        sendBufferForFile(filename, buffer);
        resultHandler.handle(new AsyncResult<Void>() {
          @Override
          public Void result() {
            return null;
          }

          @Override
          public Throwable cause() {
            return null;
          }

          @Override
          public boolean succeeded() {
            return true;
          }

          @Override
          public boolean failed() {
            return false;
          }
        });
        return this;
      })
      .orElseGet(() -> {
      wrapped.sendFile(filename, offset, length, resultHandler);
      return this;
    });
  }

  private HttpServerResponse sendBufferForFile(String filename, Buffer buffer) {
    prepareHeaders(buffer);
    setContentType(filename);
    wrapped.end(buffer);
    return this;
  }

  @Override
  public void close() {
    wrapped.close();
  }

  @Override
  public boolean ended() {
    return wrapped.ended();
  }

  @Override
  public boolean closed() {
    return wrapped.closed();
  }

  @Override
  public boolean headWritten() {
    return wrapped.headWritten();
  }

  @Override
  public HttpServerResponse headersEndHandler(Handler<Void> handler) {
    wrapped.headersEndHandler(handler);
    return this;
  }

  @Override
  public HttpServerResponse bodyEndHandler(Handler<Void> handler) {
    wrapped.bodyEndHandler(handler);
    return this;
  }

  private void setContentType(String filename) {
    int li = filename.lastIndexOf('.');
    if (li != -1 && li != filename.length() - 1) {
      String ext = filename.substring(li + 1, filename.length());
      String contentType = MimeMapping.getMimeTypeForExtension(ext);
      if (contentType != null) {
        putHeader(HttpHeaders.CONTENT_TYPE, contentType);
      }
    }
  }

  private void prepareHeaders(Buffer buffer) {
    putHeader(HttpHeaders.CONTENT_LENGTH, String.valueOf(buffer.length()));
    headers().set(HttpHeaders.CONNECTION, HttpHeaders.CLOSE);
  }

  private String appendClose(String chunk) {
    return chunk + injectedScript;
  }

  private Optional<String> getHtmlHeader() {
    Optional<String> value = ofNullable(wrapped.headers().get("Content-Type"));
    return value.filter(s -> s.equals("text/html"));
  }

  private boolean detectHtml(Buffer data) {
    htmlDetected = htmlDetected || data.toString("UTF-8").toLowerCase().contains("<html");
    return htmlDetected;
  }

  private Optional<Buffer> rewriteStaticHTMLFile(String filename, long offset, long length) {
    String lowercase = filename.toLowerCase();
    if (lowercase.endsWith(".html") || lowercase.endsWith(".htm")) {
      File file = fileResolver.apply(filename);
      if (offset + length >= file.length()) {
        return rewriteStaticHTMLFile(file, offset, length);
      }
    }
    return empty();
  }

  private Optional<Buffer> rewriteStaticHTMLFile(File file, long offset, long length) {
    try {
      Buffer buffer = Buffer.buffer();

      try (DataInputStream dis = new DataInputStream(new FileInputStream(file))) {
        // eat bytes
        while (offset > 0) {
          long max = Math.max(offset, (long) Integer.MAX_VALUE);
          dis.skipBytes((int) max);
          offset -= max;
        }
        byte[] bytes = new byte[2048];

        while ((length > 0)) {
          int read = dis.read(bytes);
          if (read <= 0) {
            length = 0;
          } else {
            length -= read;
            buffer.appendBytes(bytes, 0, read);
          }
        }
      }
      buffer.appendString(injectedScript);
      return of(buffer);
    } catch (IOException e) {
      e.printStackTrace();
      return empty();
    }
  }
}

package io.dazraf.service;

import io.vertx.core.AbstractVerticle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicInteger;

public class ChildService extends AbstractVerticle {
  private static final Logger logger = LoggerFactory.getLogger(ChildService.class);
  private static AtomicInteger atomicInteger = new AtomicInteger(0);
  @Override
  public void start() throws Exception {
    logger.info("This number should always be 0: {}", atomicInteger.getAndIncrement());
  }
}

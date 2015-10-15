package io.dazraf.service;

import io.vertx.core.AbstractVerticle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.atomic.AtomicInteger;

public class TimeService extends AbstractVerticle {
  private static final Logger logger = LoggerFactory.getLogger(TimeService.class);
  private static AtomicInteger atomicInteger = new AtomicInteger(0);
  private static SimpleDateFormat formatter = new SimpleDateFormat("HH:mm:ss");
  private long timerId;

  @Override
  public void start() throws Exception {
    logger.info("This number should always be 0: {}", atomicInteger.getAndIncrement());
    timerId = vertx.setPeriodic(500, id -> {
      String time = formatter.format(new Date());
      vertx.eventBus().publish("time", time);
    });
  }

  @Override
  public void stop() throws Exception {
    vertx.cancelTimer(timerId);
  }
}

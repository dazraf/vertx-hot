package io.fuzz.vertx.maven;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Utils {
  private final static Logger logger = LoggerFactory.getLogger(Utils.class);
  public static String getCWD() {
    return System.getProperty("user.dir");
  }
}

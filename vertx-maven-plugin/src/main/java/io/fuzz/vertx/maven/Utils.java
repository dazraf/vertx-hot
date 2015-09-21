package io.fuzz.vertx.maven;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class Utils {
  private final static Logger logger = LoggerFactory.getLogger(Utils.class);
  static String getCWD() {
    return System.getProperty("user.dir");
  }
}

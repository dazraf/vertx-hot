package io.dazraf.service;

import io.vertx.ext.web.RoutingContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;

public class AppController {
  private static final Logger logger = LoggerFactory.getLogger(AppController.class);

  private boolean flag = false;

  public AppController() {
  }
  
  public String getHostTime() {
    return LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_TIME);
  }

  public String getHostname() {
    return "hidden";
//    try {
//      return InetAddress.getLocalHost().getHostName();
//    } catch (UnknownHostException e) {
//      return e.getMessage();
//    }
  }

  /**
   * This is an example of a method that's called by the web routing
   *
   * @param context the routing context
   */
  public void createStory(RoutingContext context) {
    context.response().setChunked(true);
    context.response().write(
      "<!DOCTYPE html>" +
      "<html lang=\"en\">" +
      "<head>" +
      "    <title>Index</title>" +
      "    <link rel=\"stylesheet\" href=\"/components/bootstrap/dist/css/bootstrap.min.css\">" +
      "    <link rel=\"stylesheet\" href=\"http://bootswatch.com/paper/bootstrap.min.css\"/>" +
      "</head>" +
      "<body>" +
      "<div class=\"jumbotron\">" +
      "<div class=\"container\">" +
      "<h2>Try Refresh ... </h2>"
      );
    if (!flag) {
      context.response().write("<p>This is a simple result that tells the story</p>");
    } else {
      context.response().write("<p>This is another story</p>");
    }
    context.response().end(
      "<a class=\"btn btn-primary\" href=\"/\" role=\"button\">Go Back</a>" +
      "</div></div>" +
      "<script src=\"/components/jquery/dist/jquery.min.js\"></script>" +
      "<script src=\"/components/bootstrap/dist/js/bootstrap.min.js\"></script>" +
      "</body>" +
      "</html>");
    flag = !flag;
  }
}

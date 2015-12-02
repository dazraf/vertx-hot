package io.dazraf.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.vertx.ext.web.RoutingContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class AppController {

  private boolean flag = false; // used to flip the message from

  public AppController() {
  }

  public void close() {

  }

  public String getHostTime() {
    return LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_TIME);
  }

  public String getHostname() {
    return "hidden";
    // TRY uncommenting this (and comment out the line above)
    // // n.b. this isn't a great way of getting the hostname ...
    // // but never mind
    // try {
    //   return InetAddress.getLocalHost().getHostName();
    // } catch (UnknownHostException e) {
    //   return e.getMessage();
    // }
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

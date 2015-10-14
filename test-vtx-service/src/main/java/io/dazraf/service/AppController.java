package io.dazraf.service;

import io.vertx.ext.web.RoutingContext;

import java.net.InetAddress;
import java.net.UnknownHostException;

public class AppController {
  private boolean flag = false;

  public AppController() {}

  /**
   * This is an example of a property that will be used by a tempplate
   * @return the hostname of the server, kinda
   */
  public String getDodgyHostname() {
    try {
      return InetAddress.getLocalHost().getHostName();
    } catch (UnknownHostException e) {
      return e.getMessage();
    }
  }

  /**
   * This is an example of a method that's called by the web routing
   * @param context the routing context
   */
  public void createStory(RoutingContext context) {
    context.response().setChunked(true);
    context.response().write(
      "<!DOCTYPE html>" +
        "<html lang=\"en\">" +
        "<head>" +
        "    <title>Index</title>" +
        "    <link rel=\"stylesheet\" href=\"components/bootstrap/dist/css/bootstrap.min.css\">" +
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
        "<script src=\"components/jquery/dist/jquery.min.js\"></script>" +
        "<script src=\"components/bootstrap/dist/js/bootstrap.min.js\"></script>" +
        "</body>" +
        "</html>");
    flag = !flag;
  }
}

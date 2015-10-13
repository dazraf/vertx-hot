package io.dazraf.service;

import io.vertx.core.Vertx;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.StaticHandler;
import io.vertx.ext.web.templ.HandlebarsTemplateEngine;

import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * I've created this class to specify routes
 * Vertx is not opinionated about structure
 * Flexibility vs self-discipline
 * Here we set up routes in a type-safe manner
 * We could use a config file for the routes
 * But I prefer real type-safe code
 */
public class Routes {
  public static Router create(Vertx vertx, App app) {
    Router router = Router.router(vertx);

    bindJavaHandlerPaths(app, router);
    bindBowerComponents(router);
    bindHandlebarTemplates(router);
    bindStaticAppFiles(router);

    return router;
  }

  private static void bindBowerComponents(Router router) {
    router.route("/components/*")
      .handler(StaticHandler
        .create()
        .setWebRoot("bower_components"));
  }

  private static void bindStaticAppFiles(Router router) {
    router.route().handler(StaticHandler
      .create()
      .setCachingEnabled(false)
      .setWebRoot("static"));
  }

  private static void bindJavaHandlerPaths(App app, Router router) {
    router.get("/api/test").handler(app::test);
  }

  private static void bindHandlebarTemplates(Router router) {
    HandlebarsTemplateEngine engine = HandlebarsTemplateEngine.create();
    engine.setMaxCacheSize(0);
    router.get("/dynamic/:name").handler(ctx -> {
      try {
        ctx.put("dodgyhostname", InetAddress.getLocalHost().getHostName());
      } catch (UnknownHostException e) {
        ctx.put("dodgyhostname", e.getMessage());
      }
      String name = ctx.request().getParam("name");
      engine.render(ctx, "templates/" + name + ".hbs", res -> {
        if (res.succeeded()) {
          ctx.response().end(res.result());
        } else {
          ctx.fail(res.cause());
        }
      });
    });
  }
}

package io.fuzz.service;

import io.vertx.core.Vertx;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.StaticHandler;
import io.vertx.ext.web.templ.HandlebarsTemplateEngine;

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

    router.get("/api/test").handler(app::test);

    HandlebarsTemplateEngine engine = HandlebarsTemplateEngine.create();
    engine.setMaxCacheSize(0);
    router.get("/dynamic/:name").handler(ctx -> {
      String name = ctx.request().getParam("name");
      engine.render(ctx, "templates/" + name + ".hbs", res -> {
        if (res.succeeded()) {
          ctx.response().end(res.result());
        } else {
          ctx.fail(res.cause());
        }
      });
    });

    router.route().handler(StaticHandler
      .create()
      .setWebRoot("static"));

    return router;
  }
}

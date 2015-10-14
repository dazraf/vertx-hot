package io.dazraf.service.utils.routing;

import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.StaticHandler;
import io.vertx.ext.web.templ.HandlebarsTemplateEngine;

public class RouteMaster {
  public static void bindStatic(Router router, String urlPath, String localPath) {
    router.route(urlPath)
      .handler(StaticHandler
        .create()
        .setWebRoot(localPath));
  }

  public static void bindStatic(Router router, String localPath) {
    router.route().handler(StaticHandler
      .create()
      .setCachingEnabled(false)
      .setWebRoot(localPath));
  }

  public static void bindHandlebarTemplates(Router router, Object controller,
                                            String rootPath,
                                            String templatesLocation) {
    HandlebarsTemplateEngine engine = HandlebarsTemplateEngine.create();
    engine.setMaxCacheSize(0);

    String param = "name";
    router.get(rootPath + "/:" + param).handler(ctx -> {
      ReflectingRoutingContext dynamicCtx = new ReflectingRoutingContext(ctx, controller);
      String name = dynamicCtx.request().getParam(param);
      engine.render(dynamicCtx, templatesLocation + "/" + name + ".hbs", res -> {
        if (res.succeeded()) {
          dynamicCtx.response().end(res.result());
        } else {
          dynamicCtx.fail(res.cause());
        }
      });
    });
  }
}

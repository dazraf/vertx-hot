package io.dazraf.example.buck.web;

import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;

import java.util.List;

import static java.util.Arrays.asList;

public class NewsController {

  private List<Article> articles = asList(
    new Article("Buck up", "Your note is greener"),
    new Article("You're supported", "From gradle to cave"),
    new Article("Clean often", "Maven can't abide dust"),
    new Article("Unfortunately", "There is an ant on your ivy"),
    new Article("Return of the", "Makefile")
  );

  private int lastArticleIdx = -1;

  public void randomArticle(RoutingContext ctx) {
    Article article = nextArticle();
    ctx.response().setChunked(true);
    ctx.response().putHeader("content-type", "application/json");
    ctx.response().end(new JsonObject()
      .put("headline", article.getHeadline())
      .put("article", article.getContent())
      .toString());
  }

  private Article nextArticle() {
    if (++lastArticleIdx >= articles.size()) {
      lastArticleIdx = 0;
    }
    return articles.get(lastArticleIdx);
  }

}

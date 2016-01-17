package io.dazraf.example.buck.web;

public class Article {

  private final String headline;
  private final String content;

  public Article(String headline, String content) {
    this.headline = headline;
    this.content = content;
  }

  public String getContent() {
    return content;
  }

  public String getHeadline() {
    return headline;
  }

}

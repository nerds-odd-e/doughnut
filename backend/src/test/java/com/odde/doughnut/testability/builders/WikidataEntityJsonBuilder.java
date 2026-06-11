package com.odde.doughnut.testability.builders;

public class WikidataEntityJsonBuilder {
  private String id = "1";
  private String title = "abc";
  private String enwikiUrl = null;
  private String enwikiTitle = null;

  public WikidataEntityJsonBuilder entityId(String id) {
    this.id = id;
    return this;
  }

  public WikidataEntityJsonBuilder entitleTitle(String title) {
    this.title = title;
    return this;
  }

  public WikidataEntityJsonBuilder enwiki(String enwikiUrl) {
    this.enwikiUrl = enwikiUrl;
    return this;
  }

  public WikidataEntityJsonBuilder enwikiTitleOnly(String enwikiTitle) {
    this.enwikiTitle = enwikiTitle;
    return this;
  }

  public String please() {
    return "{\"entities\":{\""
        + id
        + "\":{\"labels\":{\"en\":{\"language\":\"en\",\"value\":\""
        + title
        + "\"}},\"sitelinks\":{"
        + enwikiJson()
        + "}}}}";
  }

  private String enwikiJson() {
    if (enwikiUrl != null) {
      return "\"enwiki\":{\"site\":\"enwiki\",\"title\":\"Mohawk language\",\"badges\":[],\"url\":\""
          + enwikiUrl
          + "\"}";
    }
    if (enwikiTitle != null) {
      return "\"enwiki\":{\"site\":\"enwiki\",\"title\":\"" + enwikiTitle + "\",\"badges\":[]}";
    }
    return "";
  }
}

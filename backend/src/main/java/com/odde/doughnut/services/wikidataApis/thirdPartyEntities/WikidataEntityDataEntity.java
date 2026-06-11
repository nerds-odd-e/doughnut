package com.odde.doughnut.services.wikidataApis.thirdPartyEntities;

import java.util.Map;

public class WikidataEntityDataEntity {
  public Map<String, LanguageValueModel> labels;
  public Map<String, WikiSiteLinkModel> sitelinks;

  public String GetEnglishWikipediaUrl() {
    String englishWikipediaTag = "enwiki";
    if (this.sitelinks == null || !this.sitelinks.containsKey(englishWikipediaTag)) {
      return "";
    }
    WikiSiteLinkModel enwiki = this.sitelinks.get(englishWikipediaTag);
    if (enwiki.url != null && !enwiki.url.isBlank()) {
      return enwiki.url;
    }
    if (enwiki.title != null && !enwiki.title.isBlank()) {
      return englishWikipediaUrlFromTitle(enwiki.title);
    }
    return "";
  }

  private static String englishWikipediaUrlFromTitle(String title) {
    return "https://en.wikipedia.org/wiki/" + title.trim().replace(" ", "_");
  }

  public String GetEnglishTitle() {
    String englishLanguageTag = "en";
    if (this.labels != null && this.labels.containsKey(englishLanguageTag)) {
      return this.labels.get(englishLanguageTag).value;
    }
    return "";
  }

  public static class LanguageValueModel {
    public String language;
    public String value;
  }

  public static class WikiSiteLinkModel {
    public String title;
    public String url;
  }
}

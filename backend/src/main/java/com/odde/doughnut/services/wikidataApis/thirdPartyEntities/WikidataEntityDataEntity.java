package com.odde.doughnut.services.wikidataApis.thirdPartyEntities;

import java.util.Map;

public class WikidataEntityDataEntity {
  public Map<String, LanguageValueModel> labels;
  public Map<String, WikiSiteLinkModel> sitelinks;

  public String GetEnglishWikipediaUrl() {
    String englishWikipediaTag = "enwiki";
    if (this.sitelinks != null && this.sitelinks.containsKey(englishWikipediaTag)) {
      return this.sitelinks.get(englishWikipediaTag).url;
    }
    return "";
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
    public String url;
  }
}

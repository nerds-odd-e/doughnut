package com.odde.doughnut.models;

import com.odde.doughnut.entities.json.WikiDataDto;
import java.util.Map;

public class WikiDataInfo {
  public String title;
  public Map<String, LanguageValueModel> labels;
  public Map<String, WikiSiteLinkModel> sitelinks;

  public WikiDataDto processInfo() {
    return new WikiDataDto(this.title, GetEnglishTitle(), GetEnglishWikipediaUrl());
  }

  private String GetEnglishWikipediaUrl() {
    String englishWikipediaTag = "enwiki";
    if (this.sitelinks != null && this.sitelinks.containsKey(englishWikipediaTag)) {
      return this.sitelinks.get(englishWikipediaTag).url;
    }
    return "";
  }

  private String GetEnglishTitle() {
    String englishLanguageTag = "en";
    if (this.labels != null && this.labels.containsKey(englishLanguageTag)) {
      return this.labels.get(englishLanguageTag).value;
    }
    return "";
  }
}

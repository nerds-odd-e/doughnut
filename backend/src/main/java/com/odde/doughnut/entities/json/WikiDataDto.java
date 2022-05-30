package com.odde.doughnut.entities.json;

public class WikiDataDto {
  public String WikiDataId;
  public String WikiDataTitleInEnglish;
  public String WikipediaEnglishUrl;

  public WikiDataDto(String wikiDataId, String wikiDataTitleInEnglish, String wikipediaEnglishUrl) {
    this.WikiDataId = wikiDataId;
    this.WikiDataTitleInEnglish = wikiDataTitleInEnglish;
    this.WikipediaEnglishUrl = wikipediaEnglishUrl;
  }
}

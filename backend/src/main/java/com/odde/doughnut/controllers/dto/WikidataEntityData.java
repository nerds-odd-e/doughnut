package com.odde.doughnut.controllers.dto;

import jakarta.validation.constraints.NotNull;

public class WikidataEntityData {
  @NotNull public String WikidataTitleInEnglish;
  @NotNull public String WikipediaEnglishUrl;

  public WikidataEntityData(String wikidataTitleInEnglish, String wikipediaEnglishUrl) {
    this.WikidataTitleInEnglish = wikidataTitleInEnglish;
    this.WikipediaEnglishUrl = wikipediaEnglishUrl;
  }
}

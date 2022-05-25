package com.odde.doughnut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;

import com.odde.doughnut.entities.json.WikiDataDto;
import com.odde.doughnut.models.LanguageValueModel;
import com.odde.doughnut.models.WikiDataInfo;
import com.odde.doughnut.models.WikiDataModel;
import com.odde.doughnut.models.WikiSiteLinkModel;
import com.odde.doughnut.services.WikiDataService;

import java.io.IOException;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.platform.commons.util.StringUtils;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

class WikiDataControllerTests {

  RestWikidataController controller;
  @Mock
  WikiDataService wikiDataService;
  private final String wikiDataIdWithNoWikipediaLink = "Q1234";
  private final String wikiDataIdWithWikipediaLink = "Q200";
  private final String noteTitle = "TDD";
  private final String englishLanguage = "en";

  private final String englishWikipediaKey = "enwiki";
  private final String englishWikipediaLink = "SomeEnglishLink";

  @BeforeEach
  void Setup() throws IOException, InterruptedException {
    MockitoAnnotations.openMocks(this);
    controller = new RestWikidataController(wikiDataService);
    WikiDataModel dummy = GenerateDummyWikiModel();
    Mockito.when(wikiDataService.FetchWikiData(any())).thenReturn(dummy);
  }

  private WikiDataModel GenerateDummyWikiModel() {
    return new WikiDataModel() {
      {
        entities =
          Map.ofEntries(
            Map.entry(
              wikiDataIdWithNoWikipediaLink,
              new WikiDataInfo() {
                {
                  title = noteTitle;
                  labels =
                    Map.ofEntries(
                      Map.entry(
                        englishLanguage,
                        new LanguageValueModel() {
                          {
                            language = englishLanguage;
                            value = noteTitle;
                          }
                        }));
                }
              }),
            Map.entry(
              wikiDataIdWithWikipediaLink,
              new WikiDataInfo() {
                {
                  title = noteTitle;
                  labels =
                    Map.ofEntries(
                      Map.entry(
                        englishLanguage,
                        new LanguageValueModel() {
                          {
                            language = englishLanguage;
                            value = noteTitle;
                          }
                        }));
                  sitelinks =
                    Map.ofEntries(
                      Map.entry(
                        englishWikipediaKey,
                        new WikiSiteLinkModel() {
                          {
                            url = englishWikipediaLink;
                          }
                        }));
                }
              }));
      }
    };
  }

  @Test
  void GivenSearchIdHasValue_ShouldBeAbleToGetWikiDataTitleAndId()
    throws IOException, InterruptedException {
    WikiDataDto resultObj = controller.fetchWikiDataDto(wikiDataIdWithNoWikipediaLink);
    assertThat(resultObj.WikiDataId, equalTo(wikiDataIdWithNoWikipediaLink));
    assertThat(resultObj.WikiDataTitleInEnglish, equalTo(noteTitle));
  }

  @Test
  void GivenSearchIdHasNoValue_ShouldReturnEmpty() throws IOException, InterruptedException {
    WikiDataDto resultObj = controller.fetchWikiDataDto("Q1");
    assertThat(resultObj, samePropertyValuesAs(new WikiDataDto()));
  }

  @Test
  void GivenSearchIdHasWikipediaEnglishLink_ShouldBeAbleToRetrieveLink()
    throws IOException, InterruptedException {
    WikiDataDto resultObj = controller.fetchWikiDataDto(wikiDataIdWithWikipediaLink);
    assertThat(resultObj.WikipediaEnglishLink, equalTo(englishWikipediaLink));
  }

  @Test
  void GivenSearchIdHasNoWikipediaEnglishLink_ShouldReturnEmptyLink()
    throws IOException, InterruptedException {
    WikiDataDto resultObj = controller.fetchWikiDataDto(wikiDataIdWithNoWikipediaLink);
    assertThat(StringUtils.isBlank(resultObj.WikipediaEnglishLink), is(true));
  }
}

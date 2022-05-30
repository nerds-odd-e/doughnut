package com.odde.doughnut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;

import com.odde.doughnut.entities.json.WikiDataDto;
import com.odde.doughnut.entities.json.WikiDataSearchResponseModel;
import com.odde.doughnut.models.LanguageValueModel;
import com.odde.doughnut.models.WikiDataInfo;
import com.odde.doughnut.models.WikiDataModel;
import com.odde.doughnut.models.WikiSiteLinkModel;
import com.odde.doughnut.services.HttpClientAdapter;
import com.odde.doughnut.services.WikiDataService;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.platform.commons.util.StringUtils;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

class WikiDataControllerTests {

  RestWikidataController controller;
  @Mock WikiDataService wikiDataService;
  @Mock HttpClientAdapter httpClientAdapter;
  private final String noteTitle = "TDD";
  private final String wikiDataIdWithNoWikipediaUrl = "Q1234";
  private final String wikiDataIdWithWikipediaUrl = "Q200";
  private final String englishLanguage = "en";

  private final String englishWikipediaKey = "enwiki";
  private final String englishWikipediaUrl = "SomeEnglishUrl";

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
                    wikiDataIdWithNoWikipediaUrl,
                    new WikiDataInfo() {
                      {
                        title = wikiDataIdWithNoWikipediaUrl;
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
                    wikiDataIdWithWikipediaUrl,
                    new WikiDataInfo() {
                      {
                        title = wikiDataIdWithWikipediaUrl;
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
                                        url = englishWikipediaUrl;
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
    WikiDataDto resultObj = controller.fetchWikiDataDto(wikiDataIdWithNoWikipediaUrl);
    assertThat(resultObj.WikiDataId, equalTo(wikiDataIdWithNoWikipediaUrl));
    assertThat(resultObj.WikiDataTitleInEnglish, equalTo(noteTitle));
  }

  @Test
  void GivenSearchIdHasWikipediaEnglishLink_ShouldBeAbleToRetrieveLink()
      throws IOException, InterruptedException {
    WikiDataDto resultObj = controller.fetchWikiDataDto(wikiDataIdWithWikipediaUrl);
    assertThat(resultObj.WikipediaEnglishUrl, equalTo(englishWikipediaUrl));
  }

  @Test
  void GivenSearchIdHasNoWikipediaEnglishLink_ShouldReturnEmptyLink()
      throws IOException, InterruptedException {
    WikiDataDto resultObj = controller.fetchWikiDataDto(wikiDataIdWithNoWikipediaUrl);
    assertThat(StringUtils.isBlank(resultObj.WikipediaEnglishUrl), is(true));
  }

  @Test
  void GivenSearchName_ShouldBeAbleToGetSearchResult() throws IOException, InterruptedException {
    WikiDataService service = new WikiDataService();
    service.httpClientAdapter = httpClientAdapter;
    Mockito.when(httpClientAdapter.getResponseString(any()))
        .thenReturn(
            "{\"searchinfo\": {\"search\": \"ktv\"},\"search\": [{\"id\": \"Q406955\",\"label\": \"KTV\"}, {\"id\": \"Q1790661\",\"label\": \"Kr\\u00f6peliner-Tor-Vorstadt\"}],\"search-continue\": 7,\"success\": 1}");

    controller = new RestWikidataController(service);

    List<WikiDataSearchResponseModel> expected =
        Arrays.asList(
            new WikiDataSearchResponseModel() {
              {
                id = "Q406955";
                label = "KTV";
              }
            },
            new WikiDataSearchResponseModel() {
              {
                id = "Q1790661";
                label = "Kröpeliner-Tor-Vorstadt";
              }
            });

    List<WikiDataSearchResponseModel> resultObj = controller.searchWikiData("tdd");

    assertThat(resultObj.get(0).id, equalTo(expected.get(0).id));
    assertThat(resultObj.get(0).label, equalTo(expected.get(0).label));
    assertThat(resultObj.get(1).id, equalTo(expected.get(1).id));
    assertThat(resultObj.get(1).label, equalTo(expected.get(1).label));
  }
}

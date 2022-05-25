package com.odde.doughnut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;

import com.odde.doughnut.entities.json.WikiDataDto;
import com.odde.doughnut.entities.json.WikiDataSearchResponseModel;
import com.odde.doughnut.models.*;
import com.odde.doughnut.services.HttpClientAdapter;
import com.odde.doughnut.models.LanguageValueModel;
import com.odde.doughnut.models.WikiDataInfo;
import com.odde.doughnut.models.WikiDataModel;
import com.odde.doughnut.models.WikiSiteLinkModel;
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
  private String searchId = "Q1234";
  private String noteTitle = "TDD";
  private String language = "en";
  private final String wikiDataIdWithNoWikipediaLink = "Q1234";
  private final String wikiDataIdWithWikipediaLink = "Q200";
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

  @Test
  void GivenSearchNameMoreThan10Results_ShouldOnlyGetTop10SearchResult()
      throws IOException, InterruptedException {
    WikiDataService service = new WikiDataService();
    service.httpClientAdapter = httpClientAdapter;
    Mockito.when(httpClientAdapter.getResponseString(any()))
        .thenReturn(
            "{\"searchinfo\": {\"search\": \"ktv\"},\"search\": [{\"id\": \"1\",\"label\": \"1\"}, {\"id\": \"1\",\"label\": \"1\"},{\"id\": \"1\",\"label\": \"1\"},{\"id\": \"1\",\"label\": \"1\"},{\"id\": \"1\",\"label\": \"1\"},{\"id\": \"1\",\"label\": \"1\"},{\"id\": \"1\",\"label\": \"1\"},{\"id\": \"1\",\"label\": \"1\"},{\"id\": \"1\",\"label\": \"1\"},{\"id\": \"1\",\"label\": \"1\"},{\"id\": \"1\",\"label\": \"1\"},{\"id\": \"1\",\"label\": \"1\"}],\"search-continue\": 7,\"success\": 1}");

    controller = new RestWikidataController(service);

    List<WikiDataSearchResponseModel> resultObj = controller.searchWikiData("tdd");

    assertThat(resultObj.size(), equalTo(10));
  }
}

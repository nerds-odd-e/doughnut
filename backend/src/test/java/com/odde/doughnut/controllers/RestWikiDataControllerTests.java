package com.odde.doughnut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;

import com.odde.doughnut.entities.json.WikidataEntityData;
import com.odde.doughnut.entities.json.WikidataSearchEntity;
import com.odde.doughnut.services.HttpClientAdapter;
import com.odde.doughnut.testability.MakeMe;
import com.odde.doughnut.testability.TestabilitySettings;
import java.io.IOException;
import java.net.URI;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.platform.commons.util.StringUtils;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.validation.BindException;

class RestWikidataControllerTests {
  RestWikidataController controller;
  @Mock HttpClientAdapter httpClientAdapter;
  TestabilitySettings testabilitySettings = new TestabilitySettings();

  @BeforeEach
  void Setup() {
    MockitoAnnotations.openMocks(this);
    controller = new RestWikidataController(testabilitySettings, httpClientAdapter);
  }

  @Nested
  class FetchWikidata {

    private String getEntityDataJsonSearch(String search) {
      return getString(search, "{\"id\":\"Q64\",\"label\":\"" + search + "\"" + "}");
    }

    private String getString(String search, String entity) {
      return "{\"searchinfo\":{\"search\":\"" + search + "\"},\"search\":[" + entity + "]}";
    }

    @Test
    void serviceNotAvailable() throws IOException, InterruptedException {
      Mockito.when(httpClientAdapter.getResponseString(any())).thenThrow(new IOException());
      BindException exception =
          assertThrows(BindException.class, () -> controller.fetchWikidataEntityDataByID("Q1"));
      assertThat(exception.getErrorCount(), equalTo(1));
    }

    @Test
    void shouldFetchInfoViaWikidataApi() throws IOException, InterruptedException, BindException {
      Mockito.when(httpClientAdapter.getResponseString(any()))
          .thenReturn(
              new MakeMe().wikidataEntityJson().entityId("Q1").entitleTitle("Mohawk").please());
      controller.fetchWikidataEntityDataByID("Q1");
      Mockito.verify(httpClientAdapter)
          .getResponseString(
              URI.create("https://www.wikidata.org/wiki/Special:EntityData/Q1.json"));
    }

    @Test
    void shouldParseAndGetTheInfo() throws IOException, InterruptedException, BindException {
      Mockito.when(httpClientAdapter.getResponseString(any()))
          .thenReturn(
              new MakeMe().wikidataEntityJson().entityId("Q1").entitleTitle("Mohawk").please());
      WikidataEntityData result = controller.fetchWikidataEntityDataByID("Q1").get();
      assertThat(result.WikidataTitleInEnglish, equalTo("Mohawk"));
    }

    @Test
    void shouldRetrieveTheEnglishWikipediaLinkIfExists()
        throws IOException, InterruptedException, BindException {
      Mockito.when(httpClientAdapter.getResponseString(any()))
          .thenReturn(
              new MakeMe()
                  .wikidataEntityJson()
                  .entityId("Q13339")
                  .entitleTitle("Mohawk")
                  .enwiki("https://en.wikipedia.org/wiki/Mohawk_language")
                  .please());
      WikidataEntityData result = controller.fetchWikidataEntityDataByID("Q13339").get();
      assertThat(
          result.WikipediaEnglishUrl, equalTo("https://en.wikipedia.org/wiki/Mohawk_language"));
    }

    @Test
    void shouldReturnEmptyIfEnglishWikipediaLinkNotExist()
        throws IOException, InterruptedException, BindException {
      Mockito.when(httpClientAdapter.getResponseString(any()))
          .thenReturn(new MakeMe().wikidataEntityJson().entityId("Q13339").please());
      WikidataEntityData result = controller.fetchWikidataEntityDataByID("Q13339").get();
      assertThat(StringUtils.isBlank(result.WikipediaEnglishUrl), is(true));
    }

    @Test
    void serviceNotAvailableAtSearchWikidata() throws IOException, InterruptedException {
      Mockito.when(httpClientAdapter.getResponseString(any())).thenThrow(new IOException());
      BindException exception =
          assertThrows(BindException.class, () -> controller.searchWikidata("berlin"));
      assertThat(exception.getErrorCount(), equalTo(1));
    }

    @Test
    void shouldFetchDataAtSearchWikidata() throws IOException, InterruptedException, BindException {
      Mockito.when(httpClientAdapter.getResponseString(any()))
          .thenReturn(getEntityDataJsonSearch("berlin"));
      controller.searchWikidata("berlin");
      Mockito.verify(httpClientAdapter).getResponseString(any());
    }

    @Test
    void shouldParseAndGetAtSearchWikidata()
        throws IOException, InterruptedException, BindException {
      Mockito.when(httpClientAdapter.getResponseString(any()))
          .thenReturn(getEntityDataJsonSearch("berlin"));
      List<WikidataSearchEntity> result = controller.searchWikidata("berlin");
      assertThat(result.get(0).label, equalTo("berlin"));
    }

    @Test
    void shouldReturnEmptyAtSearchWikidata()
        throws IOException, InterruptedException, BindException {
      Mockito.when(httpClientAdapter.getResponseString(any())).thenReturn(getString("key", ""));
      List<WikidataSearchEntity> result = controller.searchWikidata("key");
      assertThat(result.size(), is(0));
    }

    @Test
    void shouldUseTheProperlyEncodedSearchKey()
        throws IOException, InterruptedException, BindException {
      Mockito.when(httpClientAdapter.getResponseString(any()))
          .thenReturn(getString("john cena", ""));
      controller.searchWikidata("john cena");
      Mockito.verify(httpClientAdapter)
          .getResponseString(
              URI.create(
                  "https://www.wikidata.org/w/api.php?action=wbsearchentities&search=john%20cena&format=json&language=en&uselang=en&type=item&limit=10"));
    }

    @Test
    void shouldUseTheProperlyEncodedSearchKeyForUnicode()
        throws IOException, InterruptedException, BindException {
      Mockito.when(httpClientAdapter.getResponseString(any())).thenReturn(getString("梵我一如", ""));
      controller.searchWikidata("梵我一如");
      Mockito.verify(httpClientAdapter)
          .getResponseString(
              URI.create(
                  "https://www.wikidata.org/w/api.php?action=wbsearchentities&search=%E6%A2%B5%E6%88%91%E4%B8%80%E5%A6%82&format=json&language=en&uselang=en&type=item&limit=10"));
    }
  }
}

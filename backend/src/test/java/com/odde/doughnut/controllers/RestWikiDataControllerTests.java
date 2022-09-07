package com.odde.doughnut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;

import com.odde.doughnut.entities.json.WikidataEntity;
import com.odde.doughnut.entities.json.WikidataSearchEntity;
import com.odde.doughnut.models.WikidataLocationModel;
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

class RestWikiDataControllerTests {
  RestWikidataController controller;
  @Mock HttpClientAdapter httpClientAdapter;
  TestabilitySettings testabilitySettings = new TestabilitySettings();

  @BeforeEach
  void Setup() {
    MockitoAnnotations.openMocks(this);
    controller = new RestWikidataController(testabilitySettings, httpClientAdapter);
  }

  @Nested
  class FetchWikiData {

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
          assertThrows(BindException.class, () -> controller.fetchWikiDataByID("Q1"));
      assertThat(exception.getErrorCount(), equalTo(1));
    }

    @Test
    void shouldFetchInfoViaWikidataApi() throws IOException, InterruptedException, BindException {
      Mockito.when(httpClientAdapter.getResponseString(any()))
          .thenReturn(
              new MakeMe().wikidataEntityJson().entityId("Q1").entitleTitle("Mohawk").please());
      controller.fetchWikiDataByID("Q1");
      Mockito.verify(httpClientAdapter)
          .getResponseString(
              URI.create("https://www.wikidata.org/wiki/Special:EntityData/Q1.json"));
    }

    @Test
    void shouldParseAndGetTheInfo() throws IOException, InterruptedException, BindException {
      Mockito.when(httpClientAdapter.getResponseString(any()))
          .thenReturn(
              new MakeMe().wikidataEntityJson().entityId("Q1").entitleTitle("Mohawk").please());
      WikidataEntity result = controller.fetchWikiDataByID("Q1");
      assertThat(result.WikiDataTitleInEnglish, equalTo("Mohawk"));
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
      WikidataEntity result = controller.fetchWikiDataByID("Q13339");
      assertThat(
          result.WikipediaEnglishUrl, equalTo("https://en.wikipedia.org/wiki/Mohawk_language"));
    }

    @Test
    void shouldReturnEmptyIfEnglishWikipediaLinkNotExist()
        throws IOException, InterruptedException, BindException {
      Mockito.when(httpClientAdapter.getResponseString(any()))
          .thenReturn(new MakeMe().wikidataEntityJson().entityId("Q13339").please());
      WikidataEntity result = controller.fetchWikiDataByID("Q13339");
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

  @Test
  void shouldCallTheWikidataUriAndReturnLocationData() throws IOException, InterruptedException {
    Mockito.when(httpClientAdapter.getResponseString(any()))
        .thenReturn(getWikidataLocationString("Q334", "1.3", "103.8"));
    controller.getWikidataLocation("Q334");
    Mockito.verify(httpClientAdapter)
        .getResponseString(
            URI.create(
                "https://www.wikidata.org/w/api.php?action=wbgetentities&ids=Q334&props=claims&format=json"));
  }

  @Test
  void shouldReturnLocationDataForSingapore() throws IOException, InterruptedException {
    Mockito.when(httpClientAdapter.getResponseString(any()))
        .thenReturn(getWikidataLocationString("Q334", "1.3", "103.8"));

    WikidataLocationModel result = controller.getWikidataLocation("Q334");

    assertThat(result.latitude(), equalTo("1.3"));
    assertThat(result.longitude(), equalTo("103.8"));
  }

  private String getWikidataLocationString(String id, String latitude, String longitude) {
    return "{\"latitude\":" + latitude + "\", \"longitude\":" + longitude + "}";
  }
}

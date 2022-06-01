package com.odde.doughnut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;

import com.odde.doughnut.entities.json.WikidataEntity;
import com.odde.doughnut.services.HttpClientAdapter;
import com.odde.doughnut.testability.TestabilitySettings;
import java.io.IOException;
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

    private String getEntityDataJsonString(String entityId, String entityTitle, String enwikiJson) {
      return "{\"entities\":{\""
          + entityId
          + "\":{\"labels\":{\"en\":{\"language\":\"en\",\"value\":\""
          + entityTitle
          + "\"}},\"sitelinks\":{"
          + enwikiJson
          + "}}}}";
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
          .thenReturn(getEntityDataJsonString("Q1", "Mohawk", ""));
      controller.fetchWikiDataByID("Q1");
      Mockito.verify(httpClientAdapter)
          .getResponseString("https://www.wikidata.org/wiki/Special:EntityData/Q1.json");
    }

    @Test
    void shouldParseAndGetTheInfo() throws IOException, InterruptedException, BindException {
      Mockito.when(httpClientAdapter.getResponseString(any()))
          .thenReturn(getEntityDataJsonString("Q1", "Mohawk", ""));
      WikidataEntity result = controller.fetchWikiDataByID("Q1");
      assertThat(result.WikiDataTitleInEnglish, equalTo("Mohawk"));
    }

    @Test
    void shouldRetrieveTheEnglishWikipediaLinkIfExists()
        throws IOException, InterruptedException, BindException {
      Mockito.when(httpClientAdapter.getResponseString(any()))
          .thenReturn(
              getEntityDataJsonString(
                  "Q13339",
                  "Mohawk",
                  "\"enwiki\":{\"site\":\"enwiki\",\"title\":\"Mohawk language\",\"badges\":[],\"url\":\"https://en.wikipedia.org/wiki/Mohawk_language\"}"));
      WikidataEntity result = controller.fetchWikiDataByID("Q13339");
      assertThat(
          result.WikipediaEnglishUrl, equalTo("https://en.wikipedia.org/wiki/Mohawk_language"));
    }

    @Test
    void shouldReturnEmptyIfEnglishWikipediaLinkNotExist()
        throws IOException, InterruptedException, BindException {
      Mockito.when(httpClientAdapter.getResponseString(any()))
          .thenReturn(getEntityDataJsonString("Q13339", "Blah", ""));
      WikidataEntity result = controller.fetchWikiDataByID("Q13339");
      assertThat(StringUtils.isBlank(result.WikipediaEnglishUrl), is(true));
    }
  }
}

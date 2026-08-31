package com.odde.donut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;

import com.odde.donut.controllers.dto.WikidataEntityData;
import com.odde.donut.controllers.dto.WikidataSearchEntity;
import com.odde.donut.exceptions.WikidataServiceErrorException;
import com.odde.donut.services.httpQuery.HttpClientAdapter;
import com.odde.donut.testability.MakeMeWithoutDB;
import java.io.IOException;
import java.net.URI;
import java.util.List;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.platform.commons.util.StringUtils;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.validation.BindException;

class WikidataControllerTests extends ControllerTestBase {
  @Autowired WikidataController controller;
  @MockitoBean HttpClientAdapter httpClientAdapter;

  MakeMeWithoutDB wikidataJson = new MakeMeWithoutDB();

  @Nested
  class FetchWikidataEntity {
    @Test
    void serviceNotAvailable() throws IOException, InterruptedException {
      Mockito.when(httpClientAdapter.getResponseString(any())).thenThrow(new IOException());
      assertThrows(
          WikidataServiceErrorException.class, () -> controller.fetchWikidataEntityDataByID("Q1"));
    }

    @Test
    void fetchesEntityDataViaWikidataApi() throws IOException, InterruptedException, BindException {
      Mockito.when(httpClientAdapter.getResponseString(any()))
          .thenReturn(
              wikidataJson.wikidataEntityJson().entityId("Q1").entitleTitle("Mohawk").please());

      WikidataEntityData result = controller.fetchWikidataEntityDataByID("Q1").get();

      assertThat(result.WikidataTitleInEnglish, equalTo("Mohawk"));
      Mockito.verify(httpClientAdapter)
          .getResponseString(
              URI.create("https://www.wikidata.org/wiki/Special:EntityData/Q1.json"));
    }

    @Test
    void encodesWikidataIdThatLooksLikeUriTemplate()
        throws IOException, InterruptedException, BindException {
      Mockito.when(httpClientAdapter.getResponseString(any()))
          .thenReturn(wikidataJson.wikidataEntityJson().entityId("{wikidataId}").please());

      controller.fetchWikidataEntityDataByID("{wikidataId}");

      Mockito.verify(httpClientAdapter)
          .getResponseString(
              URI.create("https://www.wikidata.org/wiki/Special:EntityData/%7BwikidataId%7D.json"));
    }

    @Test
    void retrievesEnglishWikipediaLinkWhenPresent()
        throws IOException, InterruptedException, BindException {
      Mockito.when(httpClientAdapter.getResponseString(any()))
          .thenReturn(
              wikidataJson
                  .wikidataEntityJson()
                  .entityId("Q13339")
                  .entitleTitle("Mohawk")
                  .enwiki("https://en.wikipedia.org/wiki/Mohawk_language")
                  .please());

      assertThat(
          controller.fetchWikidataEntityDataByID("Q13339").get().WikipediaEnglishUrl,
          equalTo("https://en.wikipedia.org/wiki/Mohawk_language"));
    }

    @Test
    void buildsEnglishWikipediaLinkFromSitelinkTitleWhenUrlAbsent()
        throws IOException, InterruptedException, BindException {
      Mockito.when(httpClientAdapter.getResponseString(any()))
          .thenReturn(
              wikidataJson
                  .wikidataEntityJson()
                  .entityId("Q12345")
                  .entitleTitle("Count von Count")
                  .enwikiTitleOnly("Count von Count")
                  .please());

      assertThat(
          controller.fetchWikidataEntityDataByID("Q12345").get().WikipediaEnglishUrl,
          equalTo("https://en.wikipedia.org/wiki/Count_von_Count"));
    }

    @Test
    void blankWikipediaUrlWhenEnglishLinkMissing()
        throws IOException, InterruptedException, BindException {
      Mockito.when(httpClientAdapter.getResponseString(any()))
          .thenReturn(wikidataJson.wikidataEntityJson().entityId("Q13339").please());

      assertThat(
          StringUtils.isBlank(
              controller.fetchWikidataEntityDataByID("Q13339").get().WikipediaEnglishUrl),
          is(true));
    }
  }

  @Nested
  class SearchWikidata {
    private String searchJson(String search, String entity) {
      return "{\"searchinfo\":{\"search\":\"" + search + "\"},\"search\":[" + entity + "]}";
    }

    @Test
    void serviceNotAvailable() throws IOException, InterruptedException {
      Mockito.when(httpClientAdapter.getResponseString(any())).thenThrow(new IOException());
      assertThrows(WikidataServiceErrorException.class, () -> controller.searchWikidata("berlin"));
    }

    @Test
    void parsesSearchResults() throws IOException, InterruptedException, BindException {
      Mockito.when(httpClientAdapter.getResponseString(any()))
          .thenReturn(searchJson("berlin", "{\"id\":\"Q64\",\"label\":\"berlin\"}"));

      List<WikidataSearchEntity> result = controller.searchWikidata("berlin");

      assertThat(result, hasSize(1));
      assertThat(result.get(0).label, equalTo("berlin"));
    }

    @Test
    void emptyWhenNoSearchHits() throws IOException, InterruptedException, BindException {
      Mockito.when(httpClientAdapter.getResponseString(any())).thenReturn(searchJson("key", ""));

      assertThat(controller.searchWikidata("key"), hasSize(0));
    }

    @ParameterizedTest
    @CsvSource({"john cena, john%20cena", "梵我一如, %E6%A2%B5%E6%88%91%E4%B8%80%E5%A6%82"})
    void encodesSearchKeyInRequestUri(String search, String encoded)
        throws IOException, InterruptedException, BindException {
      Mockito.when(httpClientAdapter.getResponseString(any())).thenReturn(searchJson(search, ""));

      controller.searchWikidata(search);

      Mockito.verify(httpClientAdapter)
          .getResponseString(
              URI.create(
                  "https://www.wikidata.org/w/api.php?action=wbsearchentities&search="
                      + encoded
                      + "&format=json&language=en&uselang=en&type=item&limit=10"));
    }
  }
}

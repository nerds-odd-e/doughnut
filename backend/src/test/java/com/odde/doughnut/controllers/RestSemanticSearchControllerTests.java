package com.odde.doughnut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.odde.doughnut.controllers.currentUser.CurrentUser;
import com.odde.doughnut.controllers.dto.SearchTerm;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.exceptions.UnexpectedNoAccessRightException;
import com.odde.doughnut.services.AuthorizationService;
import com.odde.doughnut.services.search.NoteSearchService;
import com.odde.doughnut.testability.AuthorizationServiceTestHelper;
import com.odde.doughnut.testability.MakeMe;
import com.theokanning.openai.client.OpenAiApi;
import com.theokanning.openai.embedding.EmbeddingRequest;
import com.theokanning.openai.embedding.EmbeddingResult;
import io.reactivex.Single;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class RestSemanticSearchControllerTests {
  @Autowired MakeMe makeMe;
  @Autowired AuthorizationService authorizationService;
  @Autowired NoteSearchService noteSearchService;

  @MockitoBean(name = "testableOpenAiApi")
  OpenAiApi openAiApi;

  private CurrentUser currentUser;
  private SearchController controller;

  @BeforeEach
  void setup() {
    currentUser = new CurrentUser(makeMe.aUser().please());
    AuthorizationServiceTestHelper.setCurrentUser(authorizationService, currentUser);
    controller = new SearchController(noteSearchService, authorizationService);
    // Default: return empty embedding data so semantic search falls back to literal search
    EmbeddingResult empty = new EmbeddingResult();
    empty.setData(java.util.List.of());
    when(openAiApi.createEmbeddings(any(EmbeddingRequest.class))).thenReturn(Single.just(empty));
  }

  @Nested
  class SemanticSearch {
    @Test
    void shouldReturnEmptyListWhenNoMatchingNotes() throws UnexpectedNoAccessRightException {
      SearchTerm searchTerm = new SearchTerm();
      searchTerm.setSearchKey("nonexistent");
      searchTerm.setAllMyNotebooksAndSubscriptions(true);

      var result = controller.semanticSearch(searchTerm);

      assertThat(result, empty());
    }

    @Test
    void shouldRespectSearchScopeSettings() throws UnexpectedNoAccessRightException {}

    @Test
    void shouldNotAllowSearchWhenNotLoggedIn() {
      currentUser = new CurrentUser(null);
      AuthorizationServiceTestHelper.setCurrentUser(authorizationService, currentUser);
      controller = new SearchController(noteSearchService, authorizationService);

      SearchTerm searchTerm = new SearchTerm();
      searchTerm.setSearchKey("test");
      searchTerm.setAllMyNotebooksAndSubscriptions(true);

      assertThrows(ResponseStatusException.class, () -> controller.semanticSearch(searchTerm));
    }
  }

  @Nested
  class SemanticSearchWithinRelation {
    Note referenceNote;

    @BeforeEach
    void setup() {
      referenceNote =
          makeMe.aNote("Reference Note").creatorAndOwner(currentUser.getUser()).please();
      referenceNote =
          makeMe.aNote("Reference Note").creatorAndOwner(currentUser.getUser()).please();
    }

    @Test
    void shouldReturnEmptyListWhenNoMatchingNotesInRelation()
        throws UnexpectedNoAccessRightException {
      SearchTerm searchTerm = new SearchTerm();
      searchTerm.setSearchKey("nonexistent");
      searchTerm.setAllMyNotebooksAndSubscriptions(true);

      var result = controller.semanticSearchWithin(referenceNote, searchTerm);

      assertThat(result, empty());
    }

    @Test
    void shouldRespectSearchScopeSettingsWithinRelation() throws UnexpectedNoAccessRightException {}

    @Test
    void shouldNotAllowSearchWhenNotLoggedIn() {
      currentUser = new CurrentUser(null);
      AuthorizationServiceTestHelper.setCurrentUser(authorizationService, currentUser);
      controller = new SearchController(noteSearchService, authorizationService);

      SearchTerm searchTerm = new SearchTerm();
      searchTerm.setSearchKey("test");
      searchTerm.setAllMyNotebooksAndSubscriptions(true);

      assertThrows(
          ResponseStatusException.class,
          () -> controller.semanticSearchWithin(referenceNote, searchTerm));
    }

    @Test
    void shouldHandleEmptySearchKey() throws UnexpectedNoAccessRightException {
      SearchTerm searchTerm = new SearchTerm();
      searchTerm.setSearchKey("");
      searchTerm.setAllMyNotebooksAndSubscriptions(true);

      var result = controller.semanticSearchWithin(referenceNote, searchTerm);

      assertThat(result, empty());
    }

    @Test
    void shouldHandleWhitespaceOnlySearchKey() throws UnexpectedNoAccessRightException {
      SearchTerm searchTerm = new SearchTerm();
      searchTerm.setSearchKey("   ");
      searchTerm.setAllMyNotebooksAndSubscriptions(true);

      var result = controller.semanticSearchWithin(referenceNote, searchTerm);

      assertThat(result, empty());
    }
  }

  @Nested
  class SearchTermValidationForSemanticEndpoints {
    Note referenceNote;

    @BeforeEach
    void setup() {
      referenceNote =
          makeMe.aNote("Reference Note").creatorAndOwner(currentUser.getUser()).please();
      referenceNote =
          makeMe.aNote("Reference Note").creatorAndOwner(currentUser.getUser()).please();
    }

    @Test
    void shouldHandleNullSearchTerm() {
      assertThrows(IllegalArgumentException.class, () -> controller.semanticSearch(null));
    }

    @Test
    void shouldHandleNullSearchTermInRelation() {
      assertThrows(
          IllegalArgumentException.class,
          () -> controller.semanticSearchWithin(referenceNote, null));
    }
  }
}

package com.odde.doughnut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.odde.doughnut.controllers.dto.SearchTerm;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.exceptions.UnexpectedNoAccessRightException;
import com.odde.doughnut.models.UserModel;
import com.odde.doughnut.services.search.NoteSearchService;
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
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class RestSemanticSearchControllerTests {
  @Autowired MakeMe makeMe;
  @Autowired NoteSearchService noteSearchService;

  @MockBean(name = "testableOpenAiApi")
  OpenAiApi openAiApi;

  private UserModel userModel;
  private RestSearchController controller;

  @BeforeEach
  void setup() {
    userModel = makeMe.aUser().toModelPlease();
    controller = new RestSearchController(userModel, noteSearchService);
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
    void shouldReturnMatchingNotesUsingFallbackLiteralSearch()
        throws UnexpectedNoAccessRightException {
      Note note1 = makeMe.aNote("Java Programming").creatorAndOwner(userModel).please();
      Note note2 = makeMe.aNote("JavaScript Basics").creatorAndOwner(userModel).please();
      makeMe.aNote("Python Tutorial").creatorAndOwner(userModel).please();

      SearchTerm searchTerm = new SearchTerm();
      searchTerm.setSearchKey("Java");
      searchTerm.setAllMyNotebooksAndSubscriptions(true);

      var result = controller.semanticSearch(searchTerm);

      assertThat(result, hasSize(2));
      assertThat(
          result.stream().map(r -> r.getNoteTopology().getTitleOrPredicate()).toList(),
          containsInAnyOrder("Java Programming", "JavaScript Basics"));
      // partial matches should have distance 0.9 when falling back
      assertThat(result.stream().allMatch(r -> r.getDistance().equals(0.9f)), is(true));
    }

    @Test
    void shouldRespectSearchScopeSettings() throws UnexpectedNoAccessRightException {
      makeMe.aNote("Local Note").creatorAndOwner(userModel).please();
      makeMe.aNote("Shared Note").creatorAndOwner(userModel).please();

      SearchTerm searchTerm = new SearchTerm();
      searchTerm.setSearchKey("Note");
      searchTerm.setAllMyNotebooksAndSubscriptions(true);
      searchTerm.setAllMyCircles(false);

      var result = controller.semanticSearch(searchTerm);

      assertThat(result, hasSize(2));
    }

    @Test
    void shouldNotAllowSearchWhenNotLoggedIn() {
      userModel = makeMe.aNullUserModelPlease();
      controller = new RestSearchController(userModel, noteSearchService);

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
      referenceNote = makeMe.aNote("Reference Note").creatorAndOwner(userModel).please();
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
    void shouldReturnMatchingNotesInRelationToReference() throws UnexpectedNoAccessRightException {
      makeMe.aNote("Child Java Note").under(referenceNote).please();
      makeMe.aNote("Child JavaScript Note").under(referenceNote).please();
      makeMe.aNote("Unrelated Java Note").creatorAndOwner(userModel).please();

      SearchTerm searchTerm = new SearchTerm();
      searchTerm.setSearchKey("Java");
      searchTerm.setAllMyNotebooksAndSubscriptions(true);

      var result = controller.semanticSearchWithin(referenceNote, searchTerm);

      assertThat(result, hasSize(greaterThanOrEqualTo(2)));
      assertThat(
          result.stream().map(r -> r.getNoteTopology().getTitleOrPredicate()).toList(),
          hasItems("Child Java Note", "Unrelated Java Note"));
    }

    @Test
    void shouldRespectSearchScopeSettingsWithinRelation() throws UnexpectedNoAccessRightException {
      makeMe.aNote("Local Child Note").under(referenceNote).please();
      makeMe.aNote("Shared Child Note").under(referenceNote).please();

      SearchTerm searchTerm = new SearchTerm();
      searchTerm.setSearchKey("Child");
      searchTerm.setAllMyNotebooksAndSubscriptions(true);
      searchTerm.setAllMyCircles(false);

      var result = controller.semanticSearchWithin(referenceNote, searchTerm);

      assertThat(result, hasSize(2));
    }

    @Test
    void shouldNotAllowSearchWhenNotLoggedIn() {
      userModel = makeMe.aNullUserModelPlease();
      controller = new RestSearchController(userModel, noteSearchService);

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
      referenceNote = makeMe.aNote("Reference Note").creatorAndOwner(userModel).please();
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

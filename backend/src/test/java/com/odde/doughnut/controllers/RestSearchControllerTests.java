package com.odde.doughnut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;

import com.odde.doughnut.controllers.dto.SearchTerm;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.exceptions.UnexpectedNoAccessRightException;
import com.odde.doughnut.models.UserModel;
import com.odde.doughnut.services.AuthorizationService;
import com.odde.doughnut.services.search.NoteSearchService;
import com.odde.doughnut.testability.MakeMe;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class SearchControllerTests {
  @Autowired MakeMe makeMe;
  @Autowired AuthorizationService authorizationService;
  @Autowired NoteSearchService noteSearchService;

  private UserModel userModel;
  private SearchController controller;

  @BeforeEach
  void setup() {
    userModel = makeMe.aUser().toModelPlease();
    controller = new SearchController(userModel, noteSearchService, authorizationService);
  }

  @Nested
  class SearchForLinkTarget {
    @Test
    void shouldReturnEmptyListWhenNoMatchingNotes() throws UnexpectedNoAccessRightException {
      SearchTerm searchTerm = new SearchTerm();
      searchTerm.setSearchKey("nonexistent");
      searchTerm.setAllMyNotebooksAndSubscriptions(true);

      var result = controller.searchForLinkTarget(searchTerm);

      assertThat(result, empty());
    }

    @Test
    void shouldReturnMatchingNotes() throws UnexpectedNoAccessRightException {
      Note note1 = makeMe.aNote("Java Programming").creatorAndOwner(userModel).please();
      Note note2 = makeMe.aNote("JavaScript Basics").creatorAndOwner(userModel).please();
      makeMe.aNote("Python Tutorial").creatorAndOwner(userModel).please(); // Different topic

      SearchTerm searchTerm = new SearchTerm();
      searchTerm.setSearchKey("Java");
      searchTerm.setAllMyNotebooksAndSubscriptions(true);

      var result = controller.searchForLinkTarget(searchTerm);

      assertThat(result, hasSize(2));
      assertThat(
          result.stream().map(r -> r.getNoteTopology().getTitleOrPredicate()).toList(),
          containsInAnyOrder("Java Programming", "JavaScript Basics"));
      // partial matches should have distance 0.9 for now
      assertThat(result.stream().allMatch(r -> r.getDistance().equals(0.9f)), is(true));
    }

    @Test
    void shouldSetDistanceZeroForExactMatchesAndPointNineForPartialMatches()
        throws UnexpectedNoAccessRightException {
      makeMe.aNote("Java").creatorAndOwner(userModel).please();
      makeMe.aNote("Java Programming").creatorAndOwner(userModel).please();

      SearchTerm searchTerm = new SearchTerm();
      searchTerm.setSearchKey("Java");
      searchTerm.setAllMyNotebooksAndSubscriptions(true);

      var result = controller.searchForLinkTarget(searchTerm);

      assertThat(result, hasSize(greaterThanOrEqualTo(2)));
      assertThat(
          result.stream()
              .filter(r -> r.getNoteTopology().getTitleOrPredicate().equals("Java"))
              .allMatch(r -> r.getDistance().equals(0.0f)),
          is(true));
      assertThat(
          result.stream()
              .filter(r -> !r.getNoteTopology().getTitleOrPredicate().equals("Java"))
              .allMatch(r -> r.getDistance().equals(0.9f)),
          is(true));
    }

    @Test
    void shouldRespectSearchScopeSettings() throws UnexpectedNoAccessRightException {
      Note note1 = makeMe.aNote("Local Note").creatorAndOwner(userModel).please();
      Note note2 = makeMe.aNote("Shared Note").creatorAndOwner(userModel).please();

      SearchTerm searchTerm = new SearchTerm();
      searchTerm.setSearchKey("Note");
      searchTerm.setAllMyNotebooksAndSubscriptions(true);
      searchTerm.setAllMyCircles(false);

      var result = controller.searchForLinkTarget(searchTerm);

      assertThat(result, hasSize(2));
    }

    @Test
    void shouldNotAllowSearchWhenNotLoggedIn() {
      userModel = makeMe.aNullUserModelPlease();
      controller = new SearchController(userModel, noteSearchService, authorizationService);

      SearchTerm searchTerm = new SearchTerm();
      searchTerm.setSearchKey("test");
      searchTerm.setAllMyNotebooksAndSubscriptions(true);

      assertThrows(ResponseStatusException.class, () -> controller.searchForLinkTarget(searchTerm));
    }
  }

  @Nested
  class SearchForLinkTargetWithin {
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

      var result = controller.searchForLinkTargetWithin(referenceNote, searchTerm);

      assertThat(result, empty());
    }

    @Test
    void shouldReturnMatchingNotesInRelationToReference() throws UnexpectedNoAccessRightException {
      Note child1 = makeMe.aNote("Child Java Note").under(referenceNote).please();
      Note child2 = makeMe.aNote("Child JavaScript Note").under(referenceNote).please();
      Note unrelated = makeMe.aNote("Unrelated Java Note").creatorAndOwner(userModel).please();

      SearchTerm searchTerm = new SearchTerm();
      searchTerm.setSearchKey("Java");
      searchTerm.setAllMyNotebooksAndSubscriptions(true);

      var result = controller.searchForLinkTargetWithin(referenceNote, searchTerm);

      // The search service searches in all user's notebooks, not just the reference note's children
      // So it will find "Child Java Note", "Unrelated Java Note", and potentially the reference
      // note itself
      assertThat(result, hasSize(greaterThanOrEqualTo(2)));
      assertThat(
          result.stream().map(r -> r.getNoteTopology().getTitleOrPredicate()).toList(),
          hasItems("Child Java Note", "Unrelated Java Note"));
    }

    @Test
    void shouldRespectSearchScopeSettingsWithinRelation() throws UnexpectedNoAccessRightException {
      Note child1 = makeMe.aNote("Local Child Note").under(referenceNote).please();
      Note child2 = makeMe.aNote("Shared Child Note").under(referenceNote).please();

      SearchTerm searchTerm = new SearchTerm();
      searchTerm.setSearchKey("Child");
      searchTerm.setAllMyNotebooksAndSubscriptions(true);
      searchTerm.setAllMyCircles(false);

      var result = controller.searchForLinkTargetWithin(referenceNote, searchTerm);

      assertThat(result, hasSize(2));
    }

    @Test
    void shouldNotAllowSearchWhenNotLoggedIn() {
      userModel = makeMe.aNullUserModelPlease();
      controller = new SearchController(userModel, noteSearchService, authorizationService);

      SearchTerm searchTerm = new SearchTerm();
      searchTerm.setSearchKey("test");
      searchTerm.setAllMyNotebooksAndSubscriptions(true);

      assertThrows(
          ResponseStatusException.class,
          () -> controller.searchForLinkTargetWithin(referenceNote, searchTerm));
    }

    @Test
    void shouldHandleEmptySearchKey() throws UnexpectedNoAccessRightException {
      SearchTerm searchTerm = new SearchTerm();
      searchTerm.setSearchKey("");
      searchTerm.setAllMyNotebooksAndSubscriptions(true);

      var result = controller.searchForLinkTargetWithin(referenceNote, searchTerm);

      assertThat(result, empty());
    }

    @Test
    void shouldHandleWhitespaceOnlySearchKey() throws UnexpectedNoAccessRightException {
      SearchTerm searchTerm = new SearchTerm();
      searchTerm.setSearchKey("   ");
      searchTerm.setAllMyNotebooksAndSubscriptions(true);

      var result = controller.searchForLinkTargetWithin(referenceNote, searchTerm);

      assertThat(result, empty());
    }
  }

  @Nested
  class SearchTermValidation {
    Note referenceNote;

    @BeforeEach
    void setup() {
      referenceNote = makeMe.aNote("Reference Note").creatorAndOwner(userModel).please();
    }

    @Test
    void shouldHandleNullSearchTerm() {
      assertThrows(IllegalArgumentException.class, () -> controller.searchForLinkTarget(null));
    }

    @Test
    void shouldHandleNullSearchTermInRelation() {
      assertThrows(
          IllegalArgumentException.class,
          () -> controller.searchForLinkTargetWithin(referenceNote, null));
    }
  }
}

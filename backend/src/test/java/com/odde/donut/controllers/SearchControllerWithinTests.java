package com.odde.donut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;

import com.odde.donut.controllers.dto.RelationshipLiteralSearchHit;
import com.odde.donut.entities.Note;
import com.odde.donut.entities.Notebook;
import com.odde.donut.exceptions.UnexpectedNoAccessRightException;
import com.odde.donut.testability.RelationshipLiteralSearchHits;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.web.server.ResponseStatusException;

class SearchControllerWithinTests extends SearchControllerTestBase {

  Note referenceNote;

  @BeforeEach
  void setupReference() {
    referenceNote = ownedNote("Reference Note");
  }

  @Test
  void shouldReturnEmptyListWhenNoMatchingNotesInRelation()
      throws UnexpectedNoAccessRightException {
    assertThat(
        controller.searchForRelationshipTargetWithin(referenceNote, searchTerm("nonexistent")),
        empty());
  }

  @Test
  void shouldReturnMatchingNotesInRelationToReference() throws UnexpectedNoAccessRightException {
    makeMe.aNote("Child Java Note").underSameNotebookAs(referenceNote).please();
    makeMe.aNote("Child JavaScript Note").underSameNotebookAs(referenceNote).please();
    ownedNote("Unrelated Java Note");

    var notes =
        RelationshipLiteralSearchHits.noteMatches(
            controller.searchForRelationshipTargetWithin(referenceNote, searchTerm("Java")));

    assertThat(
        notes.stream().map(r -> r.getNoteTopology().getTitle()).toList(),
        hasItems("Child Java Note", "Unrelated Java Note"));
  }

  @Test
  void shouldRespectSearchScopeSettingsWithinRelation() throws UnexpectedNoAccessRightException {
    makeMe.aNote("Local Child Note").underSameNotebookAs(referenceNote).please();
    makeMe.aNote("Shared Child Note").underSameNotebookAs(referenceNote).please();

    assertThat(
        RelationshipLiteralSearchHits.noteMatches(
            controller.searchForRelationshipTargetWithin(
                referenceNote, searchTermInMyNotebooksOnly("Child"))),
        hasSize(2));
  }

  @Test
  void shouldSuppressNotebookLiteralHitsWhenScopedWithoutGlobalFlags()
      throws UnexpectedNoAccessRightException {
    Notebook orphanNb =
        makeMe
            .aNotebook()
            .creatorAndOwner(currentUser.getUser())
            .name("OrphanNotebookTitle")
            .please();
    makeMe.aNote().notebook(orphanNb).please();

    var result =
        controller.searchForRelationshipTargetWithin(
            referenceNote, searchTermWithoutGlobalScope("Orphan"));

    assertThat(result.stream().noneMatch(RelationshipLiteralSearchHit::isNotebook), is(true));
  }

  @Test
  void shouldNotAllowSearchWhenNotLoggedIn() {
    currentUser.setUser(null);
    assertThrows(
        ResponseStatusException.class,
        () -> controller.searchForRelationshipTargetWithin(referenceNote, searchTerm("test")));
  }

  @ParameterizedTest
  @ValueSource(strings = {"", "   "})
  void shouldHandleBlankSearchKey(String searchKey) throws UnexpectedNoAccessRightException {
    assertThat(
        controller.searchForRelationshipTargetWithin(referenceNote, searchTerm(searchKey)),
        empty());
  }

  @Test
  void shouldHandleNullSearchTermInRelation() {
    assertThrows(
        IllegalArgumentException.class,
        () -> controller.searchForRelationshipTargetWithin(referenceNote, null));
  }
}

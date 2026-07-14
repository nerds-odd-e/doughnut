package com.odde.doughnut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;

import com.odde.doughnut.controllers.dto.RelationshipLiteralSearchHit;
import com.odde.doughnut.controllers.dto.SearchTerm;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.Notebook;
import com.odde.doughnut.exceptions.UnexpectedNoAccessRightException;
import com.odde.doughnut.testability.RelationshipLiteralSearchHits;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.server.ResponseStatusException;

class SearchControllerWithinTests extends ControllerTestBase {
  @Autowired SearchController controller;

  Note referenceNote;

  @BeforeEach
  void setup() {
    currentUser.setUser(makeMe.aUser().please());
    Notebook notebook = makeMe.aNotebook().creatorAndOwner(currentUser.getUser()).please();
    referenceNote = makeMe.aNote("Reference Note").notebook(notebook).please();
  }

  @Test
  void shouldReturnEmptyListWhenNoMatchingNotesInRelation()
      throws UnexpectedNoAccessRightException {
    SearchTerm searchTerm = new SearchTerm();
    searchTerm.setSearchKey("nonexistent");
    searchTerm.setAllMyNotebooksAndSubscriptions(true);

    var result = controller.searchForRelationshipTargetWithin(referenceNote, searchTerm);

    assertThat(result, empty());
  }

  @Test
  void shouldReturnMatchingNotesInRelationToReference() throws UnexpectedNoAccessRightException {
    makeMe.aNote("Child Java Note").notebook(referenceNote.getNotebook()).please();
    makeMe.aNote("Child JavaScript Note").notebook(referenceNote.getNotebook()).please();
    Notebook unrelatedNb = makeMe.aNotebook().creatorAndOwner(currentUser.getUser()).please();
    makeMe.aNote("Unrelated Java Note").notebook(unrelatedNb).please();

    SearchTerm searchTerm = new SearchTerm();
    searchTerm.setSearchKey("Java");
    searchTerm.setAllMyNotebooksAndSubscriptions(true);

    var result = controller.searchForRelationshipTargetWithin(referenceNote, searchTerm);

    var notes = RelationshipLiteralSearchHits.noteMatches(result);
    assertThat(notes, hasSize(greaterThanOrEqualTo(2)));
    assertThat(
        notes.stream().map(r -> r.getNoteTopology().getTitle()).toList(),
        hasItems("Child Java Note", "Unrelated Java Note"));
  }

  @Test
  void shouldRespectSearchScopeSettingsWithinRelation() throws UnexpectedNoAccessRightException {
    makeMe.aNote("Local Child Note").notebook(referenceNote.getNotebook()).please();
    makeMe.aNote("Shared Child Note").notebook(referenceNote.getNotebook()).please();

    SearchTerm searchTerm = new SearchTerm();
    searchTerm.setSearchKey("Child");
    searchTerm.setAllMyNotebooksAndSubscriptions(true);
    searchTerm.setAllMyCircles(false);

    var result = controller.searchForRelationshipTargetWithin(referenceNote, searchTerm);

    assertThat(RelationshipLiteralSearchHits.noteMatches(result), hasSize(2));
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

    SearchTerm searchTerm = new SearchTerm();
    searchTerm.setSearchKey("Orphan");
    searchTerm.setAllMyNotebooksAndSubscriptions(false);
    searchTerm.setAllMyCircles(false);

    var result = controller.searchForRelationshipTargetWithin(referenceNote, searchTerm);

    assertThat(result.stream().noneMatch(RelationshipLiteralSearchHit::isNotebook), is(true));
  }

  @Test
  void shouldNotAllowSearchWhenNotLoggedIn() {
    currentUser.setUser(null);

    SearchTerm searchTerm = new SearchTerm();
    searchTerm.setSearchKey("test");
    searchTerm.setAllMyNotebooksAndSubscriptions(true);

    assertThrows(
        ResponseStatusException.class,
        () -> controller.searchForRelationshipTargetWithin(referenceNote, searchTerm));
  }

  @Test
  void shouldNotAllowSearchForRelationshipTargetWithinWhenNotLoggedIn() {
    currentUser.setUser(null);
    Note note = makeMe.aNote().please();
    SearchTerm searchTerm = new SearchTerm();
    assertThrows(
        ResponseStatusException.class,
        () -> controller.searchForRelationshipTargetWithin(note, searchTerm));
  }

  @Test
  void shouldHandleEmptySearchKey() throws UnexpectedNoAccessRightException {
    SearchTerm searchTerm = new SearchTerm();
    searchTerm.setSearchKey("");
    searchTerm.setAllMyNotebooksAndSubscriptions(true);

    var result = controller.searchForRelationshipTargetWithin(referenceNote, searchTerm);

    assertThat(result, empty());
  }

  @Test
  void shouldHandleWhitespaceOnlySearchKey() throws UnexpectedNoAccessRightException {
    SearchTerm searchTerm = new SearchTerm();
    searchTerm.setSearchKey("   ");
    searchTerm.setAllMyNotebooksAndSubscriptions(true);

    var result = controller.searchForRelationshipTargetWithin(referenceNote, searchTerm);

    assertThat(result, empty());
  }

  @Test
  void shouldHandleNullSearchTermInRelation() {
    assertThrows(
        IllegalArgumentException.class,
        () -> controller.searchForRelationshipTargetWithin(referenceNote, null));
  }
}

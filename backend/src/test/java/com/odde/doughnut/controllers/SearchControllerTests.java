package com.odde.doughnut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;

import com.odde.doughnut.controllers.dto.SearchTerm;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.Notebook;
import com.odde.doughnut.entities.User;
import com.odde.doughnut.exceptions.UnexpectedNoAccessRightException;
import com.odde.doughnut.testability.RelationshipLiteralSearchHits;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.server.ResponseStatusException;

class SearchControllerTests extends ControllerTestBase {
  @Autowired SearchController controller;
  @Autowired EntityManager entityManager;

  @BeforeEach
  void setup() {
    currentUser.setUser(makeMe.aUser().please());
  }

  @Test
  void shouldReturnEmptyListWhenNoMatchingNotes() throws UnexpectedNoAccessRightException {
    SearchTerm searchTerm = new SearchTerm();
    searchTerm.setSearchKey("nonexistent");
    searchTerm.setAllMyNotebooksAndSubscriptions(true);

    var result = controller.searchForRelationshipTarget(searchTerm);

    assertThat(result, empty());
  }

  @Test
  void shouldReturnMatchingNotes() throws UnexpectedNoAccessRightException {
    makeMe.aNote("Java Programming").notebookOwnedBy(currentUser.getUser()).please();
    makeMe.aNote("JavaScript Basics").notebookOwnedBy(currentUser.getUser()).please();
    makeMe.aNote("Python Tutorial").notebookOwnedBy(currentUser.getUser()).please();

    SearchTerm searchTerm = new SearchTerm();
    searchTerm.setSearchKey("Java");
    searchTerm.setAllMyNotebooksAndSubscriptions(true);

    var result = controller.searchForRelationshipTarget(searchTerm);

    var notes = RelationshipLiteralSearchHits.noteMatches(result);
    assertThat(notes, hasSize(2));
    assertThat(
        notes.stream().map(r -> r.getNoteTopology().getTitle()).toList(),
        containsInAnyOrder("Java Programming", "JavaScript Basics"));
    assertThat(notes.stream().allMatch(r -> r.getDistance().equals(0.9f)), is(true));
    assertThat(
        notes.stream().allMatch(r -> r.getNotebookName() != null && !r.getNotebookName().isEmpty()),
        is(true));
  }

  @Test
  void shouldSetDistanceZeroForExactMatchesAndPointNineForPartialMatches()
      throws UnexpectedNoAccessRightException {
    makeMe.aNote("Java").notebookOwnedBy(currentUser.getUser()).please();
    makeMe.aNote("Java Programming").notebookOwnedBy(currentUser.getUser()).please();

    SearchTerm searchTerm = new SearchTerm();
    searchTerm.setSearchKey("Java");
    searchTerm.setAllMyNotebooksAndSubscriptions(true);

    var result = controller.searchForRelationshipTarget(searchTerm);

    var notes = RelationshipLiteralSearchHits.noteMatches(result);
    assertThat(notes, hasSize(greaterThanOrEqualTo(2)));
    assertThat(
        notes.stream()
            .filter(r -> r.getNoteTopology().getTitle().equals("Java"))
            .allMatch(r -> r.getDistance().equals(0.0f)),
        is(true));
    assertThat(
        notes.stream()
            .filter(r -> !r.getNoteTopology().getTitle().equals("Java"))
            .allMatch(r -> r.getDistance().equals(0.9f)),
        is(true));
  }

  @Test
  void asciiTildeMatchesWaveDashAndFullWidthTilde() throws UnexpectedNoAccessRightException {
    makeMe.aNote("～によると").notebookOwnedBy(currentUser.getUser()).please();
    makeMe.aNote("文法〜によると").notebookOwnedBy(currentUser.getUser()).please();

    SearchTerm searchTerm = new SearchTerm();
    searchTerm.setSearchKey("~によると");
    searchTerm.setAllMyNotebooksAndSubscriptions(true);

    var result = controller.searchForRelationshipTarget(searchTerm);

    var notes = RelationshipLiteralSearchHits.noteMatches(result);
    assertThat(
        notes.stream().map(r -> r.getNoteTopology().getTitle()).toList(),
        containsInAnyOrder("～によると", "文法〜によると"));
    assertThat(
        notes.stream()
            .filter(r -> r.getNoteTopology().getTitle().equals("～によると"))
            .allMatch(r -> r.getDistance().equals(0.0f)),
        is(true));
    assertThat(
        notes.stream()
            .filter(r -> r.getNoteTopology().getTitle().equals("文法〜によると"))
            .allMatch(r -> r.getDistance().equals(0.9f)),
        is(true));
  }

  @Test
  void shouldRespectSearchScopeSettings() throws UnexpectedNoAccessRightException {
    makeMe.aNote("Local Note").notebookOwnedBy(currentUser.getUser()).please();
    makeMe.aNote("Shared Note").notebookOwnedBy(currentUser.getUser()).please();

    SearchTerm searchTerm = new SearchTerm();
    searchTerm.setSearchKey("Note");
    searchTerm.setAllMyNotebooksAndSubscriptions(true);
    searchTerm.setAllMyCircles(false);

    var result = controller.searchForRelationshipTarget(searchTerm);

    assertThat(RelationshipLiteralSearchHits.noteMatches(result), hasSize(2));
  }

  @Test
  void shouldReturnFolderHitsAlongsideNoteHits() throws UnexpectedNoAccessRightException {
    var notebook = makeMe.aNotebook().creatorAndOwner(currentUser.getUser()).please();
    makeMe.aFolder().notebook(notebook).name("Trip Planning").please();
    makeMe.aNote("My Trip Planning Ideas").notebook(notebook).please();

    SearchTerm searchTerm = new SearchTerm();
    searchTerm.setSearchKey("Plann");
    searchTerm.setAllMyNotebooksAndSubscriptions(true);

    var result = controller.searchForRelationshipTarget(searchTerm);

    assertThat(
        result.stream()
            .anyMatch(
                h ->
                    h.isFolder()
                        && "Trip Planning".equals(h.getFolderName())
                        && h.getFolderId() != null),
        is(true));
    assertThat(
        RelationshipLiteralSearchHits.noteMatches(result).stream()
            .anyMatch(r -> r.getNoteTopology().getTitle().contains("Planning")),
        is(true));
  }

  @Test
  void shouldReturnNotebookHitsAlongsideNoteHits() throws UnexpectedNoAccessRightException {
    User user = currentUser.getUser();
    Notebook recipeNotebook =
        makeMe.aNotebook().creatorAndOwner(user).name("Recipe Ideas").please();
    makeMe.aNote().notebook(recipeNotebook).please();
    makeMe.aNote("My Recipe Card").notebookOwnedBy(user).please();

    SearchTerm searchTerm = new SearchTerm();
    searchTerm.setSearchKey("Recipe");
    searchTerm.setAllMyNotebooksAndSubscriptions(true);

    var result = controller.searchForRelationshipTarget(searchTerm);

    assertThat(
        result.stream()
            .anyMatch(
                h ->
                    h.isNotebook()
                        && "Recipe Ideas".equals(h.getNotebookName())
                        && h.getNotebookId() != null),
        is(true));
  }

  @Test
  void literalSearchReturnsAllMatchingNotes() throws UnexpectedNoAccessRightException {
    User user = currentUser.getUser();
    Notebook nb = makeMe.aNotebook().creatorAndOwner(user).please();
    Note designatedIndex = makeMe.aNote("IdxTok999 Welcome Page").notebook(nb).please();
    Note regularNote = makeMe.aNote("Regular IdxTok999 Topic").notebook(nb).please();

    entityManager.flush();
    entityManager.clear();

    SearchTerm searchTerm = new SearchTerm();
    searchTerm.setSearchKey("IdxTok999");
    searchTerm.setAllMyNotebooksAndSubscriptions(true);

    var result = controller.searchForRelationshipTarget(searchTerm);

    var notes = RelationshipLiteralSearchHits.noteMatches(result);
    assertThat(
        notes.stream().map(r -> r.getNoteTopology().getId()).toList(),
        containsInAnyOrder(designatedIndex.getId(), regularNote.getId()));
  }

  @Test
  void shouldNotAllowSearchWhenNotLoggedIn() {
    currentUser.setUser(null);

    SearchTerm searchTerm = new SearchTerm();
    searchTerm.setSearchKey("test");
    searchTerm.setAllMyNotebooksAndSubscriptions(true);

    assertThrows(
        ResponseStatusException.class, () -> controller.searchForRelationshipTarget(searchTerm));
  }

  @Test
  void shouldNotAllowSearchForRelationshipTargetWhenNotLoggedIn() {
    currentUser.setUser(null);
    SearchTerm searchTerm = new SearchTerm();
    assertThrows(
        ResponseStatusException.class, () -> controller.searchForRelationshipTarget(searchTerm));
  }

  @Test
  void shouldHandleNullSearchTerm() {
    assertThrows(
        IllegalArgumentException.class, () -> controller.searchForRelationshipTarget(null));
  }
}

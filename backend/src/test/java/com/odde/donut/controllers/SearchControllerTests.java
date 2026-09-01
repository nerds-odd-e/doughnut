package com.odde.donut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;

import com.odde.donut.entities.Folder;
import com.odde.donut.entities.Note;
import com.odde.donut.entities.Notebook;
import com.odde.donut.exceptions.UnexpectedNoAccessRightException;
import com.odde.donut.testability.RelationshipLiteralSearchHits;
import java.util.Objects;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

class SearchControllerTests extends SearchControllerTestBase {

  @Test
  void shouldReturnEmptyListWhenNoMatchingNotes() throws UnexpectedNoAccessRightException {
    assertThat(controller.searchForRelationshipTarget(searchTerm("nonexistent")), empty());
  }

  @Test
  void shouldReturnMatchingNotes() throws UnexpectedNoAccessRightException {
    ownedNote("Java Programming");
    ownedNote("JavaScript Basics");
    ownedNote("Python Tutorial");

    var notes =
        RelationshipLiteralSearchHits.noteMatches(
            controller.searchForRelationshipTarget(searchTerm("Java")));

    assertThat(
        notes.stream().map(r -> r.getNoteTopology().getTitle()).toList(),
        containsInAnyOrder("Java Programming", "JavaScript Basics"));
    assertThat(
        notes.stream().allMatch(r -> r.getNotebookName() != null && !r.getNotebookName().isEmpty()),
        is(true));
  }

  @Test
  void noteSearchHitShowsContainingFolderNameAndOmitsItAtNotebookRoot()
      throws UnexpectedNoAccessRightException {
    Folder recipes =
        makeMe.aFolder().notebookOwnedBy(currentUser.getUser()).name("Recipes").please();
    makeMe.aNote().title("Shared").folder(recipes).please();
    makeMe.aNote().title("Shared").notebook(recipes.getNotebook()).please();

    var notes =
        RelationshipLiteralSearchHits.noteMatches(
            controller.searchForRelationshipTarget(searchTerm("Shared")));

    assertThat(
        notes.stream().map(r -> r.getFolderName()).filter(name -> "Recipes".equals(name)).toList(),
        hasSize(1));
    assertThat(
        notes.stream().map(r -> r.getFolderName()).filter(Objects::isNull).toList(), hasSize(1));
  }

  @Test
  void shouldSetDistanceZeroForExactMatchesAndPointNineForPartialMatches()
      throws UnexpectedNoAccessRightException {
    ownedNote("Java");
    ownedNote("Java Programming");

    var notes =
        RelationshipLiteralSearchHits.noteMatches(
            controller.searchForRelationshipTarget(searchTerm("Java")));

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
    ownedNote("～によると");
    ownedNote("文法〜によると");

    var notes =
        RelationshipLiteralSearchHits.noteMatches(
            controller.searchForRelationshipTarget(searchTerm("~によると")));

    assertThat(
        notes.stream().map(r -> r.getNoteTopology().getTitle()).toList(),
        containsInAnyOrder("～によると", "文法〜によると"));
  }

  @Test
  void shouldRespectSearchScopeSettings() throws UnexpectedNoAccessRightException {
    ownedNote("Local Note");
    ownedNote("Shared Note");

    assertThat(
        RelationshipLiteralSearchHits.noteMatches(
            controller.searchForRelationshipTarget(searchTermInMyNotebooksOnly("Note"))),
        hasSize(2));
  }

  @Test
  void shouldReturnFolderHitsAlongsideNoteHits() throws UnexpectedNoAccessRightException {
    Note note = ownedNote("My Trip Planning Ideas");
    makeMe.aFolder().notebook(note.getNotebook()).name("Trip Planning").please();

    var result = controller.searchForRelationshipTarget(searchTerm("Plann"));

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
    Notebook recipeNotebook =
        makeMe.aNotebook().creatorAndOwner(currentUser.getUser()).name("Recipe Ideas").please();
    makeMe.aNote().notebook(recipeNotebook).please();
    ownedNote("My Recipe Card");

    var result = controller.searchForRelationshipTarget(searchTerm("Recipe"));

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
    Note designatedIndex = ownedNote("IdxTok999 Welcome Page");
    Note regularNote =
        makeMe.aNote("Regular IdxTok999 Topic").underSameNotebookAs(designatedIndex).please();

    var notes =
        RelationshipLiteralSearchHits.noteMatches(
            controller.searchForRelationshipTarget(searchTerm("IdxTok999")));

    assertThat(
        notes.stream().map(r -> r.getNoteTopology().getId()).toList(),
        containsInAnyOrder(designatedIndex.getId(), regularNote.getId()));
  }

  @Test
  void shouldNotAllowSearchWhenNotLoggedIn() {
    currentUser.setUser(null);
    assertThrows(
        ResponseStatusException.class,
        () -> controller.searchForRelationshipTarget(searchTerm("test")));
  }

  @Test
  void shouldHandleNullSearchTerm() {
    assertThrows(
        IllegalArgumentException.class, () -> controller.searchForRelationshipTarget(null));
  }
}

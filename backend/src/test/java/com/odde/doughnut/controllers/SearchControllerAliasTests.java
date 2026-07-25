package com.odde.doughnut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import com.odde.doughnut.controllers.dto.SearchTerm;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.Notebook;
import com.odde.doughnut.exceptions.UnexpectedNoAccessRightException;
import com.odde.doughnut.services.NoteAliasIndexService;
import com.odde.doughnut.testability.RelationshipLiteralSearchHits;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class SearchControllerAliasTests extends ControllerTestBase {
  @Autowired SearchController controller;
  @Autowired NoteAliasIndexService noteAliasIndexService;

  @BeforeEach
  void setup() {
    currentUser.setUser(makeMe.aUser().please());
  }

  @Test
  void shouldReturnNotesMatchingFrontmatterAliases() throws UnexpectedNoAccessRightException {
    Note aliasMatch = createAliasNote("Colour Theory");

    SearchTerm searchTerm = searchTermFor("color");

    var result = controller.searchForRelationshipTarget(searchTerm);

    var notes = RelationshipLiteralSearchHits.noteMatches(result);
    assertThat(
        notes.stream().map(r -> r.getNoteTopology().getId()).toList(), hasItem(aliasMatch.getId()));
  }

  @Test
  void shouldRankTitleMatchesBeforeAliasMatches() throws UnexpectedNoAccessRightException {
    Note titleMatch = makeMe.aNote("Color Atlas").notebookOwnedBy(currentUser.getUser()).please();
    Note aliasMatch = createAliasNote("Colour Theory");

    SearchTerm searchTerm = searchTermFor("color");

    var result = controller.searchForRelationshipTarget(searchTerm);

    var noteIds =
        RelationshipLiteralSearchHits.noteMatches(result).stream()
            .map(r -> r.getNoteTopology().getId())
            .toList();
    assertThat(noteIds.indexOf(titleMatch.getId()), lessThan(noteIds.indexOf(aliasMatch.getId())));
  }

  @Test
  void shouldNotDuplicateNoteWhenTitleAndAliasBothMatch() throws UnexpectedNoAccessRightException {
    Note titleAndAliasMatch = createAliasNote("Color Study");

    SearchTerm searchTerm = searchTermFor("color");

    var result = controller.searchForRelationshipTarget(searchTerm);

    assertThat(
        RelationshipLiteralSearchHits.noteMatches(result).stream()
            .filter(r -> r.getNoteTopology().getId() == titleAndAliasMatch.getId())
            .count(),
        equalTo(1L));
  }

  @Test
  void shouldSearchFrontmatterAliasesWithinRelationScope() throws UnexpectedNoAccessRightException {
    Notebook notebook = makeMe.aNotebook().creatorAndOwner(currentUser.getUser()).please();
    Note referenceNote = makeMe.aNote("Reference Note").notebook(notebook).please();
    Note sameNotebookAliasMatch = createAliasNote("Colour Theory", notebook);
    Note otherNotebookAliasMatch = createAliasNote("Other Colour");

    SearchTerm searchTerm = searchTermFor("color");
    searchTerm.setAllMyNotebooksAndSubscriptions(false);
    searchTerm.setAllMyCircles(false);

    var result = controller.searchForRelationshipTargetWithin(referenceNote, searchTerm);

    var noteIds =
        RelationshipLiteralSearchHits.noteMatches(result).stream()
            .map(r -> r.getNoteTopology().getId())
            .toList();
    assertThat(noteIds, hasItem(sameNotebookAliasMatch.getId()));
    assertThat(noteIds, not(hasItem(otherNotebookAliasMatch.getId())));
  }

  @Test
  void does_not_return_note_for_wiki_link_only_overlap_alias_token_or_inner_title()
      throws UnexpectedNoAccessRightException {
    Note overlapCarrier =
        createNoteWithAliases(
            "Hue Carrier",
            """
            ---
            aliases:
              - "[[Other Note]]"
            ---

            body
            """);

    assertNoteNotInSearchResults(overlapCarrier, "[[Other Note]]");
    assertNoteNotInSearchResults(overlapCarrier, "Other Note");
  }

  @Test
  void mixed_aliases_remain_searchable_by_plain_alias_but_not_wiki_link_overlap()
      throws UnexpectedNoAccessRightException {
    Note mixed =
        createNoteWithAliases(
            "Colour Theory",
            """
            ---
            aliases:
              - color
              - "[[Other Note]]"
            ---

            body
            """);

    var plainHits =
        RelationshipLiteralSearchHits.noteMatches(
            controller.searchForRelationshipTarget(searchTermFor("color")));
    assertThat(
        plainHits.stream().map(r -> r.getNoteTopology().getId()).toList(), hasItem(mixed.getId()));

    assertNoteNotInSearchResults(mixed, "[[Other Note]]");
    assertNoteNotInSearchResults(mixed, "Other Note");
  }

  private Note createAliasNote(String title) {
    return createNoteWithAliases(title, aliasMarkdown());
  }

  private Note createAliasNote(String title, Notebook notebook) {
    Note note = makeMe.aNote(title).notebook(notebook).content(aliasMarkdown()).please();
    noteAliasIndexService.refreshForNote(note);
    return note;
  }

  private Note createNoteWithAliases(String title, String markdown) {
    Note note =
        makeMe.aNote(title).notebookOwnedBy(currentUser.getUser()).content(markdown).please();
    noteAliasIndexService.refreshForNote(note);
    return note;
  }

  private void assertNoteNotInSearchResults(Note note, String searchKey)
      throws UnexpectedNoAccessRightException {
    var noteIds =
        RelationshipLiteralSearchHits.noteMatches(
                controller.searchForRelationshipTarget(searchTermFor(searchKey)))
            .stream()
            .map(r -> r.getNoteTopology().getId())
            .toList();
    assertThat(noteIds, not(hasItem(note.getId())));
  }

  private SearchTerm searchTermFor(String searchKey) {
    SearchTerm searchTerm = new SearchTerm();
    searchTerm.setSearchKey(searchKey);
    searchTerm.setAllMyNotebooksAndSubscriptions(true);
    return searchTerm;
  }

  private String aliasMarkdown() {
    return "---\naliases:\n  - color\n---\n\nbody";
  }
}

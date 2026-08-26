package com.odde.donut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import com.odde.donut.entities.Note;
import com.odde.donut.exceptions.UnexpectedNoAccessRightException;
import com.odde.donut.testability.RelationshipLiteralSearchHits;
import org.junit.jupiter.api.Test;

class SearchControllerAliasTests extends SearchControllerTestBase {

  @Test
  void shouldReturnNotesMatchingFrontmatterAliases() throws UnexpectedNoAccessRightException {
    Note aliasMatch = aliasNote("Colour Theory");

    assertThat(
        RelationshipLiteralSearchHits.noteMatches(
                controller.searchForRelationshipTarget(searchTerm("color")))
            .stream()
            .map(r -> r.getNoteTopology().getId())
            .toList(),
        hasItem(aliasMatch.getId()));
  }

  @Test
  void shouldRankTitleMatchesBeforeAliasMatches() throws UnexpectedNoAccessRightException {
    Note titleMatch = ownedNote("Color Atlas");
    Note aliasMatch = aliasNote("Colour Theory");

    var noteIds =
        RelationshipLiteralSearchHits.noteMatches(
                controller.searchForRelationshipTarget(searchTerm("color")))
            .stream()
            .map(r -> r.getNoteTopology().getId())
            .toList();
    assertThat(noteIds.indexOf(titleMatch.getId()), lessThan(noteIds.indexOf(aliasMatch.getId())));
  }

  @Test
  void shouldNotDuplicateNoteWhenTitleAndAliasBothMatch() throws UnexpectedNoAccessRightException {
    Note titleAndAliasMatch = aliasNote("Color Study");

    assertThat(
        RelationshipLiteralSearchHits.noteMatches(
                controller.searchForRelationshipTarget(searchTerm("color")))
            .stream()
            .filter(r -> r.getNoteTopology().getId() == titleAndAliasMatch.getId())
            .count(),
        equalTo(1L));
  }

  @Test
  void shouldSearchFrontmatterAliasesWithinRelationScope() throws UnexpectedNoAccessRightException {
    Note referenceNote = ownedNote("Reference Note");
    Note sameNotebookAliasMatch =
        makeMe.aNote("Colour Theory").underSameNotebookAs(referenceNote).aliases("color").please();
    Note otherNotebookAliasMatch = aliasNote("Other Colour");

    var noteIds =
        RelationshipLiteralSearchHits.noteMatches(
                controller.searchForRelationshipTargetWithin(
                    referenceNote, searchTermWithoutGlobalScope("color")))
            .stream()
            .map(r -> r.getNoteTopology().getId())
            .toList();
    assertThat(noteIds, hasItem(sameNotebookAliasMatch.getId()));
    assertThat(noteIds, not(hasItem(otherNotebookAliasMatch.getId())));
  }

  @Test
  void shouldNotMatchWikiLinkOverlapTokenOrInnerTitle() throws UnexpectedNoAccessRightException {
    Note overlapCarrier =
        makeMe
            .aNote("Hue Carrier")
            .notebookOwnedBy(currentUser.getUser())
            .overlapWikiLink("Other Note")
            .please();

    assertNoteNotInSearchResults(overlapCarrier, "[[Other Note]]");
    assertNoteNotInSearchResults(overlapCarrier, "Other Note");
  }

  @Test
  void shouldMatchPlainAliasButNotWikiLinkOverlap() throws UnexpectedNoAccessRightException {
    Note mixed =
        makeMe
            .aNote("Colour Theory")
            .notebookOwnedBy(currentUser.getUser())
            .aliases("color")
            .overlapWikiLink("Other Note")
            .please();

    assertThat(
        RelationshipLiteralSearchHits.noteMatches(
                controller.searchForRelationshipTarget(searchTerm("color")))
            .stream()
            .map(r -> r.getNoteTopology().getId())
            .toList(),
        hasItem(mixed.getId()));

    assertNoteNotInSearchResults(mixed, "[[Other Note]]");
    assertNoteNotInSearchResults(mixed, "Other Note");
  }

  private Note aliasNote(String title) {
    return makeMe.aNote(title).notebookOwnedBy(currentUser.getUser()).aliases("color").please();
  }

  private void assertNoteNotInSearchResults(Note note, String searchKey)
      throws UnexpectedNoAccessRightException {
    assertThat(
        RelationshipLiteralSearchHits.noteMatches(
                controller.searchForRelationshipTarget(searchTerm(searchKey)))
            .stream()
            .map(r -> r.getNoteTopology().getId())
            .toList(),
        not(hasItem(note.getId())));
  }
}

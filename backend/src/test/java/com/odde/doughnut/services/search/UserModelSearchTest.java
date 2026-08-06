package com.odde.doughnut.services.search;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import com.odde.doughnut.controllers.dto.NoteTopology;
import com.odde.doughnut.controllers.dto.RelationshipLiteralSearchHit;
import com.odde.doughnut.controllers.dto.SearchTerm;
import com.odde.doughnut.entities.Circle;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.Notebook;
import com.odde.doughnut.entities.User;
import com.odde.doughnut.testability.MakeMe;
import com.odde.doughnut.testability.RelationshipLiteralSearchHits;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class UserModelSearchTest {
  @Autowired MakeMe makeMe;
  @Autowired NoteSearchService noteSearchService;
  User user;
  Note note;
  Notebook notebook;
  final SearchTerm searchTerm = new SearchTerm();

  @BeforeEach
  void setup() {
    user = makeMe.aUser().please();
    notebook = makeMe.aNotebook().creatorAndOwner(user).please();
    note = makeMe.aNote().notebook(notebook).please();
  }

  private List<RelationshipLiteralSearchHit> search() {
    return noteSearchService.searchForNotesInRelationTo(user, searchTerm, note);
  }

  @Test
  void returnEmptyWhenNoteKeyIsBlank() {
    assertThat(search(), empty());
  }

  @Test
  void theNoteItselfIsNotIncludedInTheResult() {
    searchTerm.setSearchKey(note.getTitle());
    assertThat(RelationshipLiteralSearchHits.noteMatches(search()), empty());
  }

  @Test
  void theSearchIsCaseInsensitive() {
    Note anotherNote = makeMe.aNote("Some Note").notebook(notebook).please();
    searchTerm.setSearchKey("not");
    assertThat(
        RelationshipLiteralSearchHits.noteMatches(search()).stream()
            .map(r -> r.getNoteTopology())
            .toList(),
        contains(anotherNote.getNoteTopology()));
  }

  @Test
  void theSearchResultShouldNotIncludeSoftDeletedNote() {
    makeMe.aNote("Some Note").notebook(notebook).softDeleted().please();
    searchTerm.setSearchKey("not");
    assertThat(search(), empty());
  }

  @Test
  void searchResultShouldNotExceedTwenty() {
    String commonTitle = "CommonTitle";
    for (int i = 0; i < 25; i++) {
      makeMe.aNote(commonTitle + i).notebook(notebook).please();
    }
    searchTerm.setSearchKey("CommonTitle");
    assertThat(RelationshipLiteralSearchHits.noteMatches(search()), hasSize(lessThanOrEqualTo(20)));
  }

  @Nested
  class ThereIsANoteBookInMyCircle {
    Note noteInTheSameNotebook;
    Note noteFromMyOtherNotebook;
    Note circleNote;
    Note subscribedBazaarNote;
    final String commonPhrase = "viva la";

    @BeforeEach
    void setup() {
      User anotherUser = makeMe.aUser().please();
      subscribedBazaarNote =
          makeMe.aNote(commonPhrase + " subscription").notebookOwnedBy(anotherUser).please();
      makeMe.aBazaarNotebook(subscribedBazaarNote.getNotebook()).please();
      makeMe.aSubscription().forNotebook(subscribedBazaarNote.getNotebook()).forUser(user).please();

      noteInTheSameNotebook =
          makeMe.aNote(commonPhrase + " same notebook").notebook(notebook).please();
      Notebook otherNb = makeMe.aNotebook().creatorAndOwner(user).please();
      noteFromMyOtherNotebook =
          makeMe.aNote(commonPhrase + " other notebook").notebook(otherNb).please();
      Circle circle = makeMe.aCircle().hasMember(user).hasMember(anotherUser).please();
      circleNote = makeMe.aNote(commonPhrase + " circle").inCircle(circle).please();
    }

    @ParameterizedTest
    @CsvSource({
      "false, false, false, false, false, 1",
      "true,  false, true,  true,  false, 3",
      "true,  true,  true,  true,  true, 4",
    })
    void testSearch(
        boolean allMyNotebooksAndSubscriptions,
        boolean allMyCircle,
        boolean expectOtherNotebooks,
        boolean expectSubscription,
        boolean expectCircleNote,
        int expectedCount) {
      searchTerm.setSearchKey(commonPhrase);
      searchTerm.setAllMyNotebooksAndSubscriptions(allMyNotebooksAndSubscriptions);
      searchTerm.setAllMyCircles(allMyCircle);
      List<NoteTopology> actual =
          RelationshipLiteralSearchHits.noteMatches(search()).stream()
              .map(r -> r.getNoteTopology())
              .toList();
      assertThat(actual, hasSize(expectedCount));
      assertThat(
          actual,
          containsInAnyOrder(
              expectedNotes(expectOtherNotebooks, expectSubscription, expectCircleNote)));
    }

    Object[] expectedNotes(
        boolean expectOtherNotebooks, boolean expectSubscription, boolean expectCircleNote) {
      List<NoteTopology> result = new ArrayList<>();
      result.add(noteInTheSameNotebook.getNoteTopology());
      if (expectOtherNotebooks) result.add(noteFromMyOtherNotebook.getNoteTopology());
      if (expectSubscription) result.add(subscribedBazaarNote.getNoteTopology());
      if (expectCircleNote) result.add(circleNote.getNoteTopology());
      return result.toArray();
    }
  }
}

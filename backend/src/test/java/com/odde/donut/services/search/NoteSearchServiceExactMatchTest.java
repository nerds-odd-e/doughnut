package com.odde.donut.services.search;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import com.odde.donut.controllers.dto.NoteSearchResult;
import com.odde.donut.controllers.dto.SearchTerm;
import com.odde.donut.entities.Folder;
import com.odde.donut.entities.Note;
import com.odde.donut.entities.Notebook;
import com.odde.donut.entities.User;
import com.odde.donut.testability.MakeMe;
import com.odde.donut.testability.RelationshipLiteralSearchHits;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class NoteSearchServiceExactMatchTest {
  @Autowired MakeMe makeMe;
  @Autowired NoteSearchService noteSearchService;
  User user;
  Notebook searchNotebook;
  Note parentNote;
  final SearchTerm searchTerm = new SearchTerm();

  @BeforeEach
  void setup() {
    user = makeMe.aUser().please();
    searchNotebook = makeMe.aNotebook().creatorAndOwner(user).please();
    parentNote = makeMe.aNote().notebook(searchNotebook).please();
  }

  private List<NoteSearchResult> searchNotes() {
    return RelationshipLiteralSearchHits.noteMatches(
        noteSearchService.searchForNotesInRelationTo(user, searchTerm, parentNote));
  }

  @Nested
  class ExactMatchPrioritization {

    @Test
    void shouldPutExactMatchFirstWhenSearching() {
      makeMe.aNote("Diazepam").notebook(searchNotebook).please();
      makeMe.aNote("Lorazepam").notebook(searchNotebook).please();
      makeMe.aNote("Clonazepam").notebook(searchNotebook).please();
      Note exactMatch = makeMe.aNote("Pam").notebook(searchNotebook).please();

      searchTerm.setSearchKey("pam");
      var notes = searchNotes();

      assertThat(notes, hasSize(4));
      assertThat(notes.get(0).getNoteTopology().getTitle(), equalTo("Pam"));
      assertThat(notes.get(0).getNoteTopology().getId(), equalTo(exactMatch.getId()));
    }

    @Test
    void shouldPutMultipleExactMatchesFirstWhenSearching() {
      Folder folder1 = makeMe.aFolder().notebook(searchNotebook).name("f1").please();
      Folder folder2 = makeMe.aFolder().notebook(searchNotebook).name("f2").please();
      makeMe.aNote("Diazepam").notebook(searchNotebook).please();
      Note exactMatch1 = makeMe.aNote("Pam").folder(folder1).please();
      makeMe.aNote("Lorazepam").notebook(searchNotebook).please();
      Note exactMatch2 = makeMe.aNote("pam").folder(folder2).please();
      makeMe.aNote("Clonazepam").notebook(searchNotebook).please();

      searchTerm.setSearchKey("pam");
      var notes = searchNotes();

      assertThat(notes.get(0).getNoteTopology().getId(), equalTo(exactMatch1.getId()));
      assertThat(notes.get(1).getNoteTopology().getId(), equalTo(exactMatch2.getId()));
    }

    @Test
    void shouldIncludeExactMatchesEvenWhenMoreThan20PartialMatches() {
      for (int i = 0; i < 25; i++) {
        makeMe.aNote("Diazepam" + i).notebook(searchNotebook).please();
      }
      Note exactMatch = makeMe.aNote("Pam").notebook(searchNotebook).please();

      searchTerm.setSearchKey("pam");
      var notes = searchNotes();

      assertThat(notes, hasSize(greaterThan(20)));
      assertThat(notes.get(0).getNoteTopology().getId(), equalTo(exactMatch.getId()));
    }

    @Test
    void shouldHandleCaseInsensitiveExactMatching() {
      makeMe.aNote("Diazepam").notebook(searchNotebook).please();
      Note exactMatch = makeMe.aNote("PAM").notebook(searchNotebook).please();
      makeMe.aNote("Lorazepam").notebook(searchNotebook).please();

      searchTerm.setSearchKey("pam");
      var notes = searchNotes();

      assertThat(notes.get(0).getNoteTopology().getId(), equalTo(exactMatch.getId()));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   "})
    void shouldHandleBlankSearchKey(String searchKey) {
      makeMe.aNote("Diazepam").notebook(searchNotebook).please();
      makeMe.aNote("Pam").notebook(searchNotebook).please();

      searchTerm.setSearchKey(searchKey);
      assertThat(
          noteSearchService.searchForNotesInRelationTo(user, searchTerm, parentNote), empty());
    }

    @Test
    void shouldPrioritizeSameNotebookWhenDistancesAreEqual() {
      Note sameNotebookNote = makeMe.aNote("MatchInSame").notebook(searchNotebook).please();
      Notebook otherNb = makeMe.aNotebook().creatorAndOwner(user).please();
      makeMe.aNote("Other Head").notebook(otherNb).please();
      Note otherNotebookNote = makeMe.aNote("MatchInOther").notebook(otherNb).please();

      searchTerm.setSearchKey("Match");
      searchTerm.setAllMyNotebooksAndSubscriptions(true);

      var notes = searchNotes();
      int sameNotebookIndex =
          notes.stream()
              .map(r -> r.getNoteTopology().getId())
              .toList()
              .indexOf(sameNotebookNote.getId());
      int otherNotebookIndex =
          notes.stream()
              .map(r -> r.getNoteTopology().getId())
              .toList()
              .indexOf(otherNotebookNote.getId());
      assertThat(sameNotebookIndex, lessThan(otherNotebookIndex));
    }
  }
}

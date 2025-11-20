package com.odde.doughnut.services.search;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import com.odde.doughnut.controllers.dto.NoteSearchResult;
import com.odde.doughnut.controllers.dto.SearchTerm;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.User;
import com.odde.doughnut.testability.MakeMe;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
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
  Note parentNote;
  final SearchTerm searchTerm = new SearchTerm();

  @BeforeEach
  void setup() {
    user = makeMe.aUser().please();
    parentNote = makeMe.aNote().creatorAndOwner(user).please();
  }

  @Nested
  class ExactMatchPrioritization {

    @Test
    void shouldPutExactMatchFirstWhenSearching() {
      makeMe.aNote("Diazepam").under(parentNote).please();
      makeMe.aNote("Lorazepam").under(parentNote).please();
      makeMe.aNote("Clonazepam").under(parentNote).please();
      Note exactMatch = makeMe.aNote("Pam").under(parentNote).please();

      searchTerm.setSearchKey("pam");
      List<NoteSearchResult> results =
          noteSearchService.searchForNotesInRelationTo(user, searchTerm, parentNote);

      assertThat(results, hasSize(4));
      assertThat(results.get(0).getNoteTopology().getTitleOrPredicate(), equalTo("Pam"));
      assertThat(results.get(0).getNoteTopology().getId(), equalTo(exactMatch.getId()));
    }

    @Test
    void shouldPutMultipleExactMatchesFirstWhenSearching() {
      makeMe.aNote("Diazepam").under(parentNote).please();
      Note exactMatch1 = makeMe.aNote("Pam").under(parentNote).please();
      makeMe.aNote("Lorazepam").under(parentNote).please();
      Note exactMatch2 = makeMe.aNote("pam").under(parentNote).please();
      makeMe.aNote("Clonazepam").under(parentNote).please();

      searchTerm.setSearchKey("pam");
      List<NoteSearchResult> results =
          noteSearchService.searchForNotesInRelationTo(user, searchTerm, parentNote);

      assertThat(results, hasSize(5));
      assertThat(results.get(0).getNoteTopology().getTitleOrPredicate(), equalTo("Pam"));
      assertThat(results.get(1).getNoteTopology().getTitleOrPredicate(), equalTo("pam"));
      assertThat(results.get(0).getNoteTopology().getId(), equalTo(exactMatch1.getId()));
      assertThat(results.get(1).getNoteTopology().getId(), equalTo(exactMatch2.getId()));
    }

    @Test
    void shouldIncludeExactMatchesEvenWhenMoreThan20PartialMatches() {
      for (int i = 0; i < 25; i++) {
        makeMe.aNote("Diazepam" + i).under(parentNote).please();
      }
      Note exactMatch = makeMe.aNote("Pam").under(parentNote).please();

      searchTerm.setSearchKey("pam");
      List<NoteSearchResult> results =
          noteSearchService.searchForNotesInRelationTo(user, searchTerm, parentNote);

      assertThat(results, hasSize(greaterThan(20)));
      assertThat(results.get(0).getNoteTopology().getTitleOrPredicate(), equalTo("Pam"));
      assertThat(results.get(0).getNoteTopology().getId(), equalTo(exactMatch.getId()));
    }

    @Test
    void shouldHandleCaseInsensitiveExactMatching() {
      makeMe.aNote("Diazepam").under(parentNote).please();
      Note exactMatch = makeMe.aNote("PAM").under(parentNote).please();
      makeMe.aNote("Lorazepam").under(parentNote).please();

      searchTerm.setSearchKey("pam");
      List<NoteSearchResult> results =
          noteSearchService.searchForNotesInRelationTo(user, searchTerm, parentNote);

      assertThat(results, hasSize(3));
      assertThat(results.get(0).getNoteTopology().getTitleOrPredicate(), equalTo("PAM"));
      assertThat(results.get(0).getNoteTopology().getId(), equalTo(exactMatch.getId()));
    }

    @Test
    void shouldHandleEmptySearchKey() {
      makeMe.aNote("Diazepam").under(parentNote).please();
      makeMe.aNote("Pam").under(parentNote).please();

      searchTerm.setSearchKey("");
      List<NoteSearchResult> results =
          noteSearchService.searchForNotesInRelationTo(user, searchTerm, parentNote);

      assertThat(results, empty());
    }

    @Test
    void shouldHandleWhitespaceOnlySearchKey() {
      makeMe.aNote("Diazepam").under(parentNote).please();
      makeMe.aNote("Pam").under(parentNote).please();

      searchTerm.setSearchKey("   ");
      List<NoteSearchResult> results =
          noteSearchService.searchForNotesInRelationTo(user, searchTerm, parentNote);

      assertThat(results, empty());
    }
  }
}

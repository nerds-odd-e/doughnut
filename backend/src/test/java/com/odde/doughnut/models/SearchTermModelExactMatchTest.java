package com.odde.doughnut.models;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import com.odde.doughnut.controllers.dto.NoteTopology;
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
class SearchTermModelExactMatchTest {
  @Autowired MakeMe makeMe;
  SearchTermModel searchTermModel;
  User user;
  Note parentNote;
  final SearchTerm searchTerm = new SearchTerm();

  @BeforeEach
  void setup() {
    user = makeMe.aUser().please();
    parentNote = makeMe.aNote().creatorAndOwner(user).please();
    searchTermModel =
        new SearchTermModel(user, makeMe.modelFactoryService.noteRepository, searchTerm);
  }

  @Nested
  class ExactMatchPrioritization {

    @Test
    void shouldPutExactMatchFirstWhenSearching() {
      // Given: notes with "pam" in the title and one exact match "Pam"
      makeMe.aNote("Diazepam").under(parentNote).please();
      makeMe.aNote("Lorazepam").under(parentNote).please();
      makeMe.aNote("Clonazepam").under(parentNote).please();
      Note exactMatch = makeMe.aNote("Pam").under(parentNote).please();

      // When: searching for "pam"
      searchTerm.setSearchKey("pam");
      List<NoteTopology> results = searchTermModel.searchForNotesInRelateTo(parentNote);

      // Then: exact match should be first
      assertThat(results, hasSize(4));
      assertThat(results.get(0).getTitleOrPredicate(), equalTo("Pam"));
      assertThat(results.get(0).getId(), equalTo(exactMatch.getId()));
    }

    @Test
    void shouldPutMultipleExactMatchesFirstWhenSearching() {
      // Given: multiple exact matches and partial matches
      makeMe.aNote("Diazepam").under(parentNote).please();
      Note exactMatch1 = makeMe.aNote("Pam").under(parentNote).please();
      makeMe.aNote("Lorazepam").under(parentNote).please();
      Note exactMatch2 = makeMe.aNote("pam").under(parentNote).please();
      makeMe.aNote("Clonazepam").under(parentNote).please();

      // When: searching for "pam"
      searchTerm.setSearchKey("pam");
      List<NoteTopology> results = searchTermModel.searchForNotesInRelateTo(parentNote);

      // Then: exact matches should be first (case-insensitive)
      assertThat(results, hasSize(5));
      assertThat(results.get(0).getTitleOrPredicate(), equalTo("Pam"));
      assertThat(results.get(1).getTitleOrPredicate(), equalTo("pam"));
      assertThat(results.get(0).getId(), equalTo(exactMatch1.getId()));
      assertThat(results.get(1).getId(), equalTo(exactMatch2.getId()));
    }

    @Test
    void shouldIncludeExactMatchesEvenWhenMoreThan20PartialMatches() {
      // Given: 25 partial matches and 1 exact match
      for (int i = 0; i < 25; i++) {
        makeMe.aNote("Diazepam" + i).under(parentNote).please();
      }
      Note exactMatch = makeMe.aNote("Pam").under(parentNote).please();

      // When: searching for "pam"
      searchTerm.setSearchKey("pam");
      List<NoteTopology> results = searchTermModel.searchForNotesInRelateTo(parentNote);

      // Then: exact match should be included even though there are more than 20 partial matches
      assertThat(results, hasSize(greaterThan(20)));
      assertThat(results.get(0).getTitleOrPredicate(), equalTo("Pam"));
      assertThat(results.get(0).getId(), equalTo(exactMatch.getId()));
    }

    @Test
    void shouldHandleCaseInsensitiveExactMatching() {
      // Given: notes with different cases
      makeMe.aNote("Diazepam").under(parentNote).please();
      Note exactMatch = makeMe.aNote("PAM").under(parentNote).please();
      makeMe.aNote("Lorazepam").under(parentNote).please();

      // When: searching for "pam" (lowercase)
      searchTerm.setSearchKey("pam");
      List<NoteTopology> results = searchTermModel.searchForNotesInRelateTo(parentNote);

      // Then: exact match should be first (case-insensitive)
      assertThat(results, hasSize(3));
      assertThat(results.get(0).getTitleOrPredicate(), equalTo("PAM"));
      assertThat(results.get(0).getId(), equalTo(exactMatch.getId()));
    }

    @Test
    void shouldHandleEmptySearchKey() {
      // Given: some notes
      makeMe.aNote("Diazepam").under(parentNote).please();
      makeMe.aNote("Pam").under(parentNote).please();

      // When: searching with empty key
      searchTerm.setSearchKey("");
      List<NoteTopology> results = searchTermModel.searchForNotesInRelateTo(parentNote);

      // Then: should return empty list
      assertThat(results, empty());
    }

    @Test
    void shouldHandleWhitespaceOnlySearchKey() {
      // Given: some notes
      makeMe.aNote("Diazepam").under(parentNote).please();
      makeMe.aNote("Pam").under(parentNote).please();

      // When: searching with whitespace only
      searchTerm.setSearchKey("   ");
      List<NoteTopology> results = searchTermModel.searchForNotesInRelateTo(parentNote);

      // Then: should return empty list
      assertThat(results, empty());
    }
  }
}

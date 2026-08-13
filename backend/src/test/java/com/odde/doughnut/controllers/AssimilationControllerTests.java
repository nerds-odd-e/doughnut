package com.odde.doughnut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;

import com.odde.doughnut.controllers.dto.AssimilationNextDTO;
import com.odde.doughnut.entities.*;
import com.odde.doughnut.services.NotePropertyIndexService;
import java.sql.Timestamp;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.server.ResponseStatusException;

class AssimilationControllerTests extends ControllerTestBase {
  @Autowired AssimilationController controller;
  @Autowired NotePropertyIndexService notePropertyIndexService;

  @BeforeEach
  void setup() {
    currentUser.setUser(makeMe.aUser().please());
  }

  @Nested
  class Next {
    @Test
    void returnsOwnedNoteWhenSubscriptionDailyCapReached() {
      User user = currentUser.getUser();
      Notebook subscribedNotebook =
          makeMe.aNotebook().creatorAndOwner(makeMe.aUser().please()).please();
      Note subscriptionNote = makeMe.aNote("sub").notebook(subscribedNotebook).please();
      Note ownedNote =
          AssimilationControllerTestSupport.ownedNote(makeMe, currentUser.getUser(), "owned");
      makeMe.aSubscription().forNotebook(subscribedNotebook).forUser(user).daily(1).please();
      Timestamp day1 = makeMe.aTimestamp().of(1, 8).fromShanghai().please();
      testabilitySettings.timeTravelTo(day1);
      makeMe.aMemoryTrackerFor(subscriptionNote).by(user).assimilatedAt(day1).please();
      makeMe.refresh(user);

      AssimilationNextDTO result = controller.next("Asia/Shanghai");
      assertThat(result.getNextUnit().getNoteId(), equalTo(ownedNote.getId()));
    }

    @Test
    void returnsNullWhenNoNotesLeft() {
      assertThat(controller.next("Asia/Shanghai").getNextUnit(), nullValue());
    }

    @Test
    void countsAreCorrect() {
      AssimilationControllerTestSupport.ownedNote(makeMe, currentUser.getUser(), "note1");
      AssimilationControllerTestSupport.ownedNote(makeMe, currentUser.getUser(), "note2");

      AssimilationNextDTO result = controller.next("Asia/Shanghai");
      assertThat(result.getCounts().getDueCount(), equalTo(2));
      assertThat(result.getCounts().getAssimilatedCountOfTheDay(), equalTo(0));
      assertThat(result.getCounts().getTotalUnassimilatedCount(), equalTo(2));
    }

    @Test
    void commissionedOnlyNoteStillAppearsInOrdinaryAssimilationQueue() {
      Note note =
          AssimilationControllerTestSupport.ownedNote(
              makeMe, currentUser.getUser(), "commissioned-only");
      makeMe.aMemoryTrackerFor(note).commissioned().please();

      AssimilationNextDTO result = controller.next("Asia/Shanghai");
      assertThat(result.getNextUnit().getNoteId(), equalTo(note.getId()));
      assertThat(result.getCounts().getTotalUnassimilatedCount(), equalTo(1));
    }

    @Test
    void assimilatedCountOfTheDayExcludesCommissionedTrackers() {
      Timestamp day1 = makeMe.aTimestamp().of(1, 8).fromShanghai().please();
      testabilitySettings.timeTravelTo(day1);
      Note note =
          AssimilationControllerTestSupport.ownedNote(
              makeMe, currentUser.getUser(), "commissioned-today");
      makeMe.aMemoryTrackerFor(note).commissioned().assimilatedAt(day1).please();

      AssimilationNextDTO result = controller.next("Asia/Shanghai");
      assertThat(result.getCounts().getAssimilatedCountOfTheDay(), equalTo(0));
    }

    @Test
    void spellingOnlyNoteStillAppearsInOrdinaryAssimilationQueue() {
      Note note =
          AssimilationControllerTestSupport.ownedNote(
              makeMe, currentUser.getUser(), "spelling-only");
      makeMe.aMemoryTrackerFor(note).spelling().please();

      AssimilationNextDTO result = controller.next("Asia/Shanghai");
      assertThat(result.getNextUnit().getNoteId(), equalTo(note.getId()));
      assertThat(result.getCounts().getTotalUnassimilatedCount(), equalTo(1));
    }

    @Test
    void assimilatedCountOfTheDayExcludesSpellingTrackers() {
      Timestamp day1 = makeMe.aTimestamp().of(1, 8).fromShanghai().please();
      testabilitySettings.timeTravelTo(day1);
      Note note =
          AssimilationControllerTestSupport.ownedNote(
              makeMe, currentUser.getUser(), "spelling-today");
      makeMe.aMemoryTrackerFor(note).spelling().assimilatedAt(day1).please();

      AssimilationNextDTO result = controller.next("Asia/Shanghai");
      assertThat(result.getCounts().getAssimilatedCountOfTheDay(), equalTo(0));
    }

    @Test
    void notLoggedIn() {
      currentUser.setUser(null);
      assertThrows(ResponseStatusException.class, () -> controller.next("Asia/Shanghai"));
    }

    @Test
    void returns_next_property_key_for_untracked_example_of() {
      Timestamp day1 = makeMe.aTimestamp().of(1, 8).fromShanghai().please();
      testabilitySettings.timeTravelTo(day1);
      Note note =
          makeMe
              .aNote()
              .notebookOwnedBy(currentUser.getUser())
              .content("---\nexample of: \"[[Word]]\"\n---\n\nbody")
              .please();
      notePropertyIndexService.refreshForNote(note);
      makeMe.aMemoryTrackerFor(note).assimilatedAt(day1).please();

      AssimilationNextDTO result = controller.next("Asia/Shanghai");
      assertThat(result.getNextUnit().getNoteId(), equalTo(note.getId()));
      assertThat(result.getNextUnit().getPropertyKey(), equalTo("example of"));
    }
  }
}

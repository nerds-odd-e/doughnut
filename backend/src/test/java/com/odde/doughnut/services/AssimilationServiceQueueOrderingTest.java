package com.odde.doughnut.services;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import com.odde.doughnut.entities.*;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class AssimilationServiceQueueOrderingTest extends AssimilationServiceTestBase {

  @Test
  void whenThereIsNoNotesForUser() {
    makeMe.aNote().notebookOwnedBy(anotherUser).please();
    assertThat(getNextNoteToAssimilate(assimilationService), is(nullValue()));
    assertThat(assimilationService.getCounts().getDueCount(), equalTo(0));
  }

  @Nested
  class WhenThereAreTwoNotesForUser {
    Note note1;
    Note note2;

    @BeforeEach
    void setup() {
      note1 = makeMe.aNote("note1").notebookOwnedBy(user).please();
      note2 = makeMe.aNote("note2").notebookOwnedBy(user).please();
    }

    @Test
    void shouldReturnTheFirstNoteAndThenTheSecondWhenThereAreTwo() {
      assertThat(assimilationService.getCounts().getDueCount(), equalTo(2));
      assertThat(getNextNoteToAssimilate(assimilationService), equalTo(note1));
      makeMe.aMemoryTrackerFor(note1).by(user).assimilatedAt(day1).please();
      assertThat(assimilationService.getCounts().getDueCount(), equalTo(1));
      assertThat(getNextNoteToAssimilate(assimilationService), equalTo(note2));
    }

    @Test
    void shouldReturnTheSecondNoteIfItsLevelIsLower() {
      makeMe.theNote(note1).level(2).please();
      makeMe.theNote(note2).level(1).please();
      assertThat(getNextNoteToAssimilate(assimilationService), equalTo(note2));
    }

    @Test
    void shouldNotIncludeNoteThatIsSkippedForRecall() {
      makeMe.theNote(note1).skipMemoryTracking().please();
      assertThat(getNextNoteToAssimilate(assimilationService), equalTo(note2));
    }

    @Nested
    class MemoryTrackerFromLink {
      Note anotherNote;

      @BeforeEach
      void thereIsALinkAndAnotherNote() {
        anotherNote = makeMe.aNote("another note").notebookOwnedBy(user).please();
      }

      private List<Note> collectNextNotesInOrder() {
        List<Note> notes = new ArrayList<>();
        Optional<Note> next;
        while ((next = assimilationService.getNextNoteToAssimilate()).isPresent()) {
          Note note = next.get();
          notes.add(note);
          makeMe.aMemoryTrackerFor(note).by(user).assimilatedAt(day1).please();
        }
        return notes;
      }

      @Nested
      class WithLevels {
        @BeforeEach
        void Note1And2HaveDifferentLevels() {
          makeMe.theNote(note1).level(5).please();
          makeMe.theNote(note2).level(2).please();
        }

        @Test
        void shouldReturnMemoryTrackerForLowerLevelNoteOrLink() {
          List<Note> memoryTrackers = collectNextNotesInOrder();
          assertThat(memoryTrackers, hasSize(3));
          assertThat(memoryTrackers.get(0), equalTo(anotherNote));
          assertThat(memoryTrackers.get(1), equalTo(note2));
          assertThat(memoryTrackers.get(2), equalTo(note1));
        }

        @Test
        void shouldNotReturnMemoryTrackerForLinkIfCreatedByOtherPeople() {
          makeMe.theNote(note1).notebookOwnership(makeMe.aUser().please()).please();
          List<Note> memoryTrackers = collectNextNotesInOrder();
          assertThat(memoryTrackers, hasSize(2));
          assertThat(memoryTrackers.get(0), equalTo(anotherNote));
          assertThat(memoryTrackers.get(1), equalTo(note2));
        }
      }
    }
  }

  @Nested
  class QueueOrdering {
    Notebook subscribedNotebook;

    @BeforeEach
    void setupSubscribedNotebook() {
      User notebookOwner = makeMe.aUser().please();
      subscribedNotebook = makeMe.aNotebook().creatorAndOwner(notebookOwner).please();
      makeMe.aSubscription().forNotebook(subscribedNotebook).forUser(user).daily(10).please();
    }

    @Test
    void owned_and_subscribed_notes_interleave_by_level_not_subscription_first() {
      makeMe.aNote("subscribed").notebook(subscribedNotebook).level(5).please();
      Note ownedNote = makeMe.aNote("owned").notebookOwnedBy(user).level(1).please();
      makeMe.refresh(user);

      assertThat(getNextNoteToAssimilate(assimilationService), equalTo(ownedNote));
    }

    @Test
    void owned_and_subscribed_notes_interleave_by_created_at_when_levels_equal() {
      Timestamp earlier = makeMe.aTimestamp().of(0, 8).fromShanghai().please();
      Timestamp later = makeMe.aTimestamp().of(1, 8).fromShanghai().please();
      Note subscribedNote =
          makeMe
              .aNote("subscribed")
              .notebook(subscribedNotebook)
              .level(1)
              .createdAt(earlier)
              .please();
      makeMe.aNote("owned").notebookOwnedBy(user).level(1).createdAt(later).please();
      makeMe.refresh(user);

      assertThat(getNextNoteToAssimilate(assimilationService), equalTo(subscribedNote));
    }
  }
}

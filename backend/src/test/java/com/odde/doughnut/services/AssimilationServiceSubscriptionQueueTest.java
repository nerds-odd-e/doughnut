package com.odde.doughnut.services;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import com.odde.doughnut.entities.*;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class AssimilationServiceSubscriptionQueueTest extends AssimilationServiceTestBase {

  @Nested
  class WhenNoteHasOnlyPropertyTracker {
    Note note;

    @BeforeEach
    void setup() {
      note = makeMe.aNote("vitamins").notebookOwnedBy(user).please();
      makeMe.aMemoryTrackerFor(note).by(user).propertyKey("topic").assimilatedAt(day1).please();
    }

    @Test
    void shouldAppearInUnassimilatedNotes() {
      assertThat(
          userService.getUnassimilatedNotes(user).map(Note::getId).toList(), hasItem(note.getId()));
    }
  }

  @Nested
  class RecallSubscribedNote {
    Note note1;
    Note note2;

    @BeforeEach
    void setup() {
      User anotherUser = makeMe.aUser().please();
      Notebook topNb = makeMe.aNotebook().creatorAndOwner(anotherUser).please();
      note1 = makeMe.aNote().notebook(topNb).please();
      note2 = makeMe.aNote().notebook(topNb).please();
      makeMe.aSubscription().forNotebook(topNb).forUser(user).daily(1).please();
      makeMe.refresh(user);
    }

    @Test
    void shouldReturnMemoryTrackerForNote() {
      assertThat(getNextNoteToAssimilate(assimilationService), equalTo(note1));
    }

    @Test
    void shouldReturnMemoryTrackerForLink() {
      makeMe.theNote(note2).skipMemoryTracking().please();
      makeMe.theNote(note1).skipMemoryTracking().please();
      Note link = makeMe.aNote().notebook(note1.getNotebook()).please();
      makeMe.refresh(user);
      Subscription sub = user.getSubscriptions().stream().findFirst().orElseThrow();
      List<Integer> dueInSubscribedNotebook =
          subscriptionService.getUnassimilatedNotes(sub).map(Note::getId).toList();
      assertThat(dueInSubscribedNotebook, hasItem(link.getId()));
    }

    @Test
    void recalledMoreThanPlanned() {
      makeMe.aMemoryTrackerFor(note1).by(user).assimilatedAt(day1).please();
      makeMe.aMemoryTrackerFor(note2).by(user).assimilatedAt(day1).please();
      assertThat(getNextNoteToAssimilate(assimilationService), nullValue());
    }
  }

  @Nested
  class NotesInCircle {

    @BeforeEach
    void setup() {
      Circle please = makeMe.aCircle().hasMember(user).please();
      makeMe.aNote().inCircle(please).please();
      makeMe.refresh(user);
    }

    @Test
    void shouldNotBeRecalled() {
      assertThat(getNextNoteToAssimilate(assimilationService), is(nullValue()));
    }
  }
}

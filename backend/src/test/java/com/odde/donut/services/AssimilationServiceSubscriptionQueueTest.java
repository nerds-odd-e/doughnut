package com.odde.donut.services;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import com.odde.donut.entities.*;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class AssimilationServiceSubscriptionQueueTest extends AssimilationServiceTestBase {

  @Nested
  class WhenNoteHasOnlyPropertyTracker {
    @Test
    void shouldAppearInUnassimilatedNotes() {
      Note note = makeMe.aNote("vitamins").notebookOwnedBy(user).please();
      makeMe.aMemoryTrackerFor(note).propertyKey("topic").assimilatedAt(day1).please();

      assertThat(
          userService.getUnassimilatedNotes(user).map(unit -> unit.note().getId()).toList(),
          hasItem(note.getId()));
    }
  }

  @Nested
  class RecallSubscribedNote {
    Note note1;
    Note note2;
    Notebook topNb;

    @BeforeEach
    void setup() {
      User notebookOwner = makeMe.aUser().please();
      topNb = makeMe.aNotebook().creatorAndOwner(notebookOwner).please();
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
      makeMe.aMemoryTrackerFor(note1).by(user).removedFromTracking().please();
      makeMe.aMemoryTrackerFor(note2).by(user).removedFromTracking().please();
      Note link = makeMe.aNote().notebook(topNb).please();
      makeMe.refresh(user);
      Subscription sub = user.getSubscriptions().stream().findFirst().orElseThrow();
      List<Integer> dueInSubscribedNotebook =
          subscriptionService.getUnassimilatedNotes(sub).map(unit -> unit.note().getId()).toList();
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
    @Test
    void shouldNotBeRecalled() {
      Circle circle = makeMe.aCircle().hasMember(user).please();
      makeMe.aNote().inCircle(circle).please();
      makeMe.refresh(user);

      assertThat(getNextNoteToAssimilate(assimilationService), is(nullValue()));
    }
  }
}

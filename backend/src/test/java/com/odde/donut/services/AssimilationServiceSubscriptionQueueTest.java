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

  @Test
  void datedPropertyInSubscribedNotebookInterleavesWithOwnedNotesByDate() {
    Notebook subscribedNotebook = makeMe.aNotebook().creatorAndOwner(anotherUser).please();
    Note subscribedNote =
        makeMe
            .aNote("subscribed")
            .notebook(subscribedNotebook)
            .createdAt(makeMe.aTimestamp().of(0, 8).fromShanghai().please())
            .content("---\ntopic: physics\n---\n\nbody")
            .please();
    notePropertyIndexService.refreshForNote(subscribedNote);
    dateScalarProperty(
        subscribedNote, "topic", makeMe.aTimestamp().of(0, 10).fromShanghai().please());
    makeMe.aSubscription().forNotebook(subscribedNotebook).forUser(user).daily(5).please();
    makeMe
        .aMemoryTrackerFor(subscribedNote)
        .by(user)
        .assimilatedAt(makeMe.aTimestamp().of(0, 12).fromShanghai().please())
        .please();
    Note ownedBefore =
        makeMe
            .aNote("owned before")
            .notebookOwnedBy(user)
            .createdAt(makeMe.aTimestamp().of(0, 9).fromShanghai().please())
            .please();
    Note ownedAfter =
        makeMe
            .aNote("owned after")
            .notebookOwnedBy(user)
            .createdAt(makeMe.aTimestamp().of(0, 11).fromShanghai().please())
            .please();
    makeMe.refresh(user);

    assertThat(getNextNoteToAssimilate(assimilationService), equalTo(ownedBefore));
    makeMe.aMemoryTrackerFor(ownedBefore).by(user).assimilatedAt(day1).please();
    AssimilationUnit property = assimilationService.getNextAssimilationUnit().orElseThrow();
    assertThat(property.note(), equalTo(subscribedNote));
    assertThat(property.propertyKey(), equalTo("topic"));
    makeMe
        .aMemoryTrackerFor(subscribedNote)
        .by(user)
        .propertyKey("topic")
        .assimilatedAt(day1)
        .please();
    assertThat(getNextNoteToAssimilate(assimilationService), equalTo(ownedAfter));
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

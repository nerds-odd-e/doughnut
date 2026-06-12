package com.odde.doughnut.services;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import com.odde.doughnut.controllers.dto.AssimilationCountDTO;
import com.odde.doughnut.entities.*;
import com.odde.doughnut.testability.MakeMe;
import java.sql.Timestamp;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
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
public class AssimilationServiceTest {
  @Autowired MakeMe makeMe;
  @Autowired SubscriptionService subscriptionService;
  @Autowired UserService userService;
  @Autowired UnassimilatedPropertyService unassimilatedPropertyService;
  User user;
  User anotherUser;
  Timestamp day1;
  AssimilationService assimilationService;

  @BeforeEach
  void setup() {
    user = makeMe.aUser().please();
    anotherUser = makeMe.aUser().please();
    day1 = makeMe.aTimestamp().of(1, 8).fromShanghai().please();
    assimilationService = assimilationServiceFor(user, day1);
  }

  private AssimilationService assimilationServiceFor(User forUser, Timestamp at) {
    return new AssimilationService(
        forUser,
        userService,
        subscriptionService,
        unassimilatedPropertyService,
        at,
        ZoneId.of("Asia/Shanghai"));
  }

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

    @Nested
    class WhenTheUserSetToRecall1NewNoteOnlyPerDay {

      @BeforeEach
      void setup() {
        userService.setDailyAssimilationCount(user, 1);
      }

      @Test
      void shouldReturnOneIfUsersDailySettingIsOne() {
        assertThat(getNextNoteToAssimilate(assimilationService), equalTo(note1));
      }

      @Test
      void shouldNotCountSkippedMemoryTracker() {
        makeMe.aMemoryTrackerFor(note1).by(user).assimilatedAt(day1).removedFromTracking().please();
        assertThat(getNextNoteToAssimilate(assimilationService), is(note2));
      }

      @Test
      void shouldIncludeNotesThatAreRecalledByOtherPeople() {
        makeMe.aMemoryTrackerFor(note1).by(anotherUser).assimilatedAt(day1).please();
        assertThat(getNextNoteToAssimilate(assimilationService), equalTo(note1));
      }

      @Test
      void theDailyCountShouldNotBeResetOnSameDayDifferentHour() {
        makeMe.aMemoryTrackerFor(note1).by(user).assimilatedAt(day1).please();
        Timestamp day1_23 = makeMe.aTimestamp().of(1, 23).fromShanghai().please();
        AssimilationService recallService = assimilationServiceFor(user, day1_23);
        assertThat(getNextNoteToAssimilate(recallService), equalTo(note2));
      }

      @Test
      void theDailyCountShouldBeResetOnNextDay() {
        makeMe.aMemoryTrackerFor(note1).by(user).assimilatedAt(day1).please();
        Timestamp day2 = makeMe.aTimestamp().of(2, 1).fromShanghai().please();
        AssimilationService recallService = assimilationServiceFor(user, day2);
        assertThat(getNextNoteToAssimilate(recallService), equalTo(note2));
      }
    }
  }

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

  @Nested
  class WhenRecalledMoreThanDailyLimitLastNight {
    Note note1;
    Note note2;
    Note note3;
    AssimilationService earlyMorningService;
    Timestamp earlyMorning;
    Timestamp lateMorning;

    @BeforeEach
    void setup() {
      makeMe.theUser(user).dailyAssimilationCount(2).please();
      // Set up subscription notes
      User anotherUser = makeMe.aUser().please();
      Notebook topNb = makeMe.aNotebook().creatorAndOwner(anotherUser).please();
      note1 = makeMe.aNote().notebook(topNb).please();
      note2 = makeMe.aNote().notebook(topNb).please();
      note3 = makeMe.aNote().notebook(topNb).please();
      makeMe.aNote().notebook(topNb).please();

      // Set up subscription with daily limit of 1
      makeMe.aSubscription().forNotebook(topNb).forUser(user).daily(1).please();

      // Set up a note that belongs to the user
      Notebook userNb = makeMe.aNotebook().creatorAndOwner(user).please();
      makeMe.aNote().notebook(userNb).please();

      makeMe.refresh(user);

      // Set up timestamps for last night 11pm and next day 6am
      earlyMorning = makeMe.aTimestamp().of(1, 6).fromShanghai().please();
      lateMorning = makeMe.aTimestamp().of(1, 10).fromShanghai().please();

      // Recall more notes than daily limit last night
      makeMe.aMemoryTrackerFor(note1).by(user).assimilatedAt(earlyMorning).please();
      makeMe.aMemoryTrackerFor(note2).by(user).assimilatedAt(earlyMorning).please();
      makeMe.aMemoryTrackerFor(note3).by(user).assimilatedAt(earlyMorning).please();

      // Create service for early morning check
      earlyMorningService = assimilationServiceFor(user, lateMorning);
    }

    @Test
    void returnsNextNotePastUserDailyCap() {
      assertThat(earlyMorningService.getNextNoteToAssimilate().isPresent(), is(true));
    }

    @Test
    void dueCountIsZeroWhenUserDailyPlanComplete() {
      AssimilationCountDTO counts = earlyMorningService.getCounts();
      assertThat(counts.getDueCount(), equalTo(0));
    }
  }

  private Note getNextNoteToAssimilate(AssimilationService service) {
    return service.getNextNoteToAssimilate().orElse(null);
  }
}

package com.odde.doughnut.models;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import com.odde.doughnut.entities.*;
import com.odde.doughnut.services.AssimilationService;
import com.odde.doughnut.testability.MakeMe;
import java.sql.Timestamp;
import java.time.ZoneId;
import java.util.List;
import java.util.stream.Collectors;
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
  UserModel userModel;
  UserModel anotherUser;
  Timestamp day1;
  Timestamp day0;
  AssimilationService assimilationService;

  @BeforeEach
  void setup() {
    userModel = makeMe.aUser().toModelPlease();
    anotherUser = makeMe.aUser().toModelPlease();
    day1 = makeMe.aTimestamp().of(1, 8).fromShanghai().please();
    day0 = makeMe.aTimestamp().of(0, 8).fromShanghai().please();
    assimilationService =
        new AssimilationService(
            userModel, makeMe.modelFactoryService, day1, ZoneId.of("Asia/Shanghai"));
  }

  @Test
  void whenThereIsNoNotesForUser() {
    makeMe.aNote().creatorAndOwner(anotherUser).please();
    assertThat(getFirstInitialMemoryTracker(assimilationService), is(nullValue()));
    assertThat(assimilationService.getCounts().getDueCount(), equalTo(0));
  }

  @Nested
  class WhenThereAreTwoNotesForUser {
    Note note1;
    Note note2;

    @BeforeEach
    void setup() {
      note1 = makeMe.aNote("note1").creatorAndOwner(userModel).please();
      note2 = makeMe.aNote("note2").creatorAndOwner(userModel).please();
    }

    @Test
    void shouldReturnTheFirstNoteAndThenTheSecondWhenThereAreTwo() {
      assertThat(assimilationService.getCounts().getDueCount(), equalTo(2));
      assertThat(getFirstInitialMemoryTracker(assimilationService), equalTo(note1));
      makeMe.aMemoryTrackerFor(note1).by(userModel).initiallyReviewedOn(day1).please();
      assertThat(assimilationService.getCounts().getDueCount(), equalTo(1));
      assertThat(getFirstInitialMemoryTracker(assimilationService), equalTo(note2));
    }

    @Test
    void shouldReturnTheSecondNoteIfItsLevelIsLower() {
      makeMe.theNote(note1).level(2).please();
      makeMe.theNote(note2).level(1).please();
      assertThat(getFirstInitialMemoryTracker(assimilationService), equalTo(note2));
    }

    @Test
    void shouldNotIncludeNoteThatIsSkippedForReview() {
      makeMe.theNote(note1).skipMemoryTracking().linkTo(note2).please();
      assertThat(getFirstInitialMemoryTracker(assimilationService), equalTo(note2));
    }

    @Nested
    class MemoryTrackerFromLink {
      Note note1ToNote2;
      Note anotherNote;

      @BeforeEach
      void thereIsALinkAndAnotherNote() {
        note1ToNote2 = makeMe.aLink().between(note1, note2).please();
        anotherNote = makeMe.aNote("another note").creatorAndOwner(userModel).please();
      }

      private List<Note> getAllDueMemoryTrackers() {
        return assimilationService.getDueInitialMemoryTrackers().collect(Collectors.toList());
      }

      @Test
      void shouldReturnLinkBeforeAnotherNote() {
        List<Note> memoryTrackers = getAllDueMemoryTrackers();
        assertThat(memoryTrackers, hasSize(4));
        assertThat(memoryTrackers.get(0), equalTo(note1));
        assertThat(memoryTrackers.get(2), equalTo(note1ToNote2));
        assertThat(memoryTrackers.get(1), equalTo(note2));
        assertThat(memoryTrackers.get(3), equalTo(anotherNote));
      }

      @Nested
      class WithLevels {
        @BeforeEach
        void Note1And2HaveDifferentLevels() {
          makeMe.theNote(note1).level(5).please();
          makeMe.theNote(note2).level(2).please();
          makeMe.theNote(note1ToNote2).level(5).please();
        }

        @Test
        void shouldReturnMemoryTrackerForLowerLevelNoteOrLink() {
          List<Note> memoryTrackers = getAllDueMemoryTrackers();
          assertThat(memoryTrackers, hasSize(4));
          assertThat(memoryTrackers.get(0), equalTo(anotherNote));
          assertThat(memoryTrackers.get(1), equalTo(note2));
          assertThat(memoryTrackers.get(2), equalTo(note1));
          assertThat(memoryTrackers.get(3), equalTo(note1ToNote2));
        }

        @Test
        void shouldReturnLinksOrderedByLevels() {
          Note aLevel2Link = makeMe.aLink().between(anotherNote, note2).please();
          List<Note> memoryTrackers = getAllDueMemoryTrackers();
          assertThat(memoryTrackers, hasSize(5));
          assertThat(memoryTrackers.get(0), equalTo(anotherNote));
          assertThat(memoryTrackers.get(1), equalTo(note2));
          assertThat(memoryTrackers.get(2), equalTo(aLevel2Link));
          assertThat(memoryTrackers.get(4), equalTo(note1ToNote2));
        }

        @Test
        void shouldNotReturnMemoryTrackerForLinkIfCreatedByOtherPeople() {
          makeMe.theNote(note1).notebookOwnership(makeMe.aUser().please()).please();
          List<Note> memoryTrackers = getAllDueMemoryTrackers();
          assertThat(memoryTrackers, hasSize(2));
          assertThat(memoryTrackers.get(0), equalTo(anotherNote));
          assertThat(memoryTrackers.get(1), equalTo(note2));
        }
      }
    }

    @Nested
    class WhenTheUserSetToReview1NewNoteOnlyPerDay {

      @BeforeEach
      void setup() {
        userModel.setAndSaveDailyNewNotesCount(1);
      }

      @Test
      void shouldReturnOneIfUsersDailySettingIsOne() {
        assertThat(getFirstInitialMemoryTracker(assimilationService), equalTo(note1));
      }

      @Test
      void shouldNotIncludeNotesThatAreAlreadyReviewed() {
        makeMe.aMemoryTrackerFor(note1).by(userModel).initiallyReviewedOn(day1).please();
        assertThat(getFirstInitialMemoryTracker(assimilationService), is(nullValue()));
      }

      @Test
      void shouldNotCountSkippedMemoryTracker() {
        makeMe
            .aMemoryTrackerFor(note1)
            .by(userModel)
            .initiallyReviewedOn(day1)
            .removedFromTracking()
            .please();
        assertThat(getFirstInitialMemoryTracker(assimilationService), is(note2));
      }

      @Test
      void shouldIncludeNotesThatAreReviewedByOtherPeople() {
        makeMe.aMemoryTrackerFor(note1).by(anotherUser).initiallyReviewedOn(day1).please();
        assertThat(getFirstInitialMemoryTracker(assimilationService), equalTo(note1));
      }

      @Test
      void theDailyCountShouldNotBeResetOnSameDayDifferentHour() {
        makeMe.aMemoryTrackerFor(note1).by(userModel).initiallyReviewedOn(day1).please();
        Timestamp day1_23 = makeMe.aTimestamp().of(1, 23).fromShanghai().please();
        AssimilationService recallService =
            new AssimilationService(
                userModel, makeMe.modelFactoryService, day1_23, ZoneId.of("Asia/Shanghai"));
        assertThat(getFirstInitialMemoryTracker(recallService), is(nullValue()));
      }

      @Test
      void theDailyCountShouldBeResetOnNextDay() {
        makeMe.aMemoryTrackerFor(note1).by(userModel).initiallyReviewedOn(day1).please();
        Timestamp day2 = makeMe.aTimestamp().of(2, 1).fromShanghai().please();
        AssimilationService recallService =
            new AssimilationService(
                userModel, makeMe.modelFactoryService, day2, ZoneId.of("Asia/Shanghai"));
        assertThat(getFirstInitialMemoryTracker(recallService), equalTo(note2));
      }
    }
  }

  @Nested
  class ReviewSubscribedNote {
    Note note1;
    Note note2;

    @BeforeEach
    void setup() {
      User anotherUser = makeMe.aUser().please();
      Note top = makeMe.aNote().skipMemoryTracking().creatorAndOwner(anotherUser).please();
      note1 = makeMe.aNote().under(top).please();
      note2 = makeMe.aNote().under(top).please();
      makeMe
          .aSubscription()
          .forNotebook(top.getNotebook())
          .forUser(userModel.entity)
          .daily(1)
          .please();
      makeMe.refresh(userModel.getEntity());
    }

    @Test
    void shouldReturnMemoryTrackerForNote() {
      assertThat(getFirstInitialMemoryTracker(assimilationService), equalTo(note1));
    }

    @Test
    void shouldReturnMemoryTrackerForLink() {
      makeMe.theNote(note2).skipMemoryTracking().please();
      makeMe.theNote(note1).skipMemoryTracking().linkTo(note2).please();
      Note noteToReview = getFirstInitialMemoryTracker(assimilationService);
      assertThat(noteToReview.getParent(), equalTo(note1));
    }

    @Test
    void reviewedMoreThanPlanned() {
      makeMe.aMemoryTrackerFor(note1).by(userModel).initiallyReviewedOn(day1).please();
      makeMe.aMemoryTrackerFor(note2).by(userModel).initiallyReviewedOn(day1).please();
      assertThat(getFirstInitialMemoryTracker(assimilationService), nullValue());
    }
  }

  @Nested
  class NotesInCircle {
    Note top;

    @BeforeEach
    void setup() {
      Circle please = makeMe.aCircle().hasMember(userModel).please();
      top = makeMe.aNote().inCircle(please).please();
      makeMe.refresh(userModel.getEntity());
    }

    @Test
    void shouldNotBeReviewed() {
      assertThat(getFirstInitialMemoryTracker(assimilationService), is(nullValue()));
    }
  }

  private Note getFirstInitialMemoryTracker(AssimilationService recallService) {
    return recallService.getDueInitialMemoryTrackers().findFirst().orElse(null);
  }
}

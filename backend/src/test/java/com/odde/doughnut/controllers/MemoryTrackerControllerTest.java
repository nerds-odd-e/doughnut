package com.odde.doughnut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.odde.doughnut.controllers.dto.AnswerSpellingDTO;
import com.odde.doughnut.controllers.dto.SelfEvaluation;
import com.odde.doughnut.controllers.dto.SpellingQuestion;
import com.odde.doughnut.controllers.dto.SpellingResultDTO;
import com.odde.doughnut.entities.MemoryTracker;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.User;
import com.odde.doughnut.exceptions.UnexpectedNoAccessRightException;
import com.odde.doughnut.factoryServices.ModelFactoryService;
import com.odde.doughnut.services.UserService;
import com.odde.doughnut.testability.TestabilitySettings;
import com.odde.doughnut.utils.TimestampOperations;
import java.sql.Timestamp;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.server.ResponseStatusException;

class MemoryTrackerControllerTest extends ControllerTestBase {
  @Autowired ModelFactoryService modelFactoryService;
  @Autowired UserService userService;

  private final TestabilitySettings testabilitySettings = new TestabilitySettings();
  MemoryTrackerController controller;

  @BeforeEach
  void setup() {
    currentUser.setUser(makeMe.aUser().please());
    controller =
        new MemoryTrackerController(
            modelFactoryService,
            makeMe.entityPersister,
            testabilitySettings,
            authorizationService,
            userService);
  }

  @Nested
  class GetSpellingQuestion {
    @Test
    void shouldReturnClozedDetailsAsQuestionStem() throws UnexpectedNoAccessRightException {
      Note note =
          makeMe
              .aNote("moon")
              .details("partner of earth")
              .creatorAndOwner(currentUser.getUser())
              .please();
      MemoryTracker memoryTracker = makeMe.aMemoryTrackerFor(note).please();

      SpellingQuestion question = controller.getSpellingQuestion(memoryTracker);
      assertThat(question.getStem(), equalTo("<p>partner of earth</p>\n"));
    }

    @Test
    void shouldNotBeAbleToGetSpellingQuestionForOthersMemoryTracker() {
      MemoryTracker memoryTracker = makeMe.aMemoryTrackerBy(makeMe.aUser().please()).please();
      assertThrows(
          UnexpectedNoAccessRightException.class,
          () -> controller.getSpellingQuestion(memoryTracker));
    }

    @Test
    void shouldRequireUserToBeLoggedIn() {
      currentUser.setUser(null);
      controller =
          new MemoryTrackerController(
              modelFactoryService,
              makeMe.entityPersister,
              testabilitySettings,
              authorizationService,
              userService);
      MemoryTracker memoryTracker = makeMe.aMemoryTrackerBy(makeMe.aUser().please()).please();
      assertThrows(
          ResponseStatusException.class, () -> controller.getSpellingQuestion(memoryTracker));
    }
  }

  @Nested
  class Show {
    @Nested
    class WhenThereIsAMemoryTracker {
      MemoryTracker rp;

      @BeforeEach
      void setup() {
        // fix the time
        testabilitySettings.timeTravelTo(makeMe.aTimestamp().please());
        rp = makeMe.aMemoryTrackerFor(makeMe.aNote().please()).by(currentUser.getUser()).please();
      }

      @Test
      void shouldBeAbleToSeeOwn() throws UnexpectedNoAccessRightException {
        MemoryTracker memoryTracker = controller.showMemoryTracker(rp);
        assertThat(memoryTracker, equalTo(rp));
      }

      @Test
      void shouldNotBeAbleToSeeOthers() {
        rp = makeMe.aMemoryTrackerBy(makeMe.aUser().please()).please();
        assertThrows(
            UnexpectedNoAccessRightException.class, () -> controller.showMemoryTracker(rp));
      }

      @Test
      void removeAndUpdateLastRecalledAt() {
        controller.removeFromRepeating(rp);
        assertThat(rp.getRemovedFromTracking(), is(true));
        assertThat(rp.getLastRecalledAt(), equalTo(testabilitySettings.getCurrentUTCTimestamp()));
      }
    }
  }

  @Nested
  class Evaluate {

    @Test
    void shouldNotBeAbleToSeeNoteIDontHaveAccessTo() {
      currentUser.setUser(null);
      MemoryTracker memoryTracker =
          makeMe.aMemoryTrackerFor(makeMe.aNote().please()).inMemoryPlease();
      SelfEvaluation selfEvaluation =
          new SelfEvaluation() {
            {
              this.adjustment = 1;
            }
          };
      assertThrows(
          ResponseStatusException.class,
          () -> controller.selfEvaluate(memoryTracker, selfEvaluation));
    }

    @Test
    void whenTheMemoryTrackerDoesNotExist() {
      SelfEvaluation selfEvaluation =
          new SelfEvaluation() {
            {
              this.adjustment = 1;
            }
          };
      assertThrows(
          ResponseStatusException.class, () -> controller.selfEvaluate(null, selfEvaluation));
    }

    @Nested
    class WhenThereIsAMemoryTracker {
      MemoryTracker rp;

      @BeforeEach
      void setup() {
        rp = makeMe.aMemoryTrackerFor(makeMe.aNote().please()).by(currentUser.getUser()).please();
      }

      @Test
      void repeat() {
        evaluate(1);
        assertThat(rp.getForgettingCurveIndex(), equalTo(101));
        assertThat(rp.getRepetitionCount(), equalTo(0));
      }

      private void evaluate(int adj) {
        SelfEvaluation selfEvaluation =
            new SelfEvaluation() {
              {
                this.adjustment = adj;
              }
            };
        controller.selfEvaluate(rp, selfEvaluation);
      }
    }
  }

  @Nested
  class MarkAsReviewed {
    @Test
    void itMustUpdateTheMemoryTrackerRecord() {
      Note note = makeMe.aNote().please();
      MemoryTracker rp = makeMe.aMemoryTrackerFor(note).by(currentUser.getUser()).please();
      Integer oldRepetitionCount = rp.getRepetitionCount();
      controller.markAsRepeated(rp, true);
      assertThat(rp.getRepetitionCount(), equalTo(oldRepetitionCount + 1));
    }
  }

  @Nested
  class GetRecentMemoryTrackers {
    @Test
    void shouldReturnEmptyListWhenNoMemoryTrackers() {
      List<MemoryTracker> memoryTrackers = controller.getRecentMemoryTrackers();
      assertThat(memoryTrackers, empty());
    }

    @Test
    void shouldReturnMemoryTrackersForCurrentUser() {
      MemoryTracker rp1 =
          makeMe.aMemoryTrackerFor(makeMe.aNote().please()).by(currentUser.getUser()).please();
      MemoryTracker rp2 =
          makeMe.aMemoryTrackerFor(makeMe.aNote().please()).by(currentUser.getUser()).please();

      List<MemoryTracker> memoryTrackers = controller.getRecentMemoryTrackers();

      assertThat(memoryTrackers, hasSize(2));
      assertThat(memoryTrackers, containsInAnyOrder(rp1, rp2));
    }

    @Test
    void shouldNotReturnMemoryTrackersFromOtherUsers() {
      User otherUser = makeMe.aUser().please();
      makeMe.aMemoryTrackerBy(otherUser).please();

      List<MemoryTracker> memoryTrackers = controller.getRecentMemoryTrackers();

      assertThat(memoryTrackers, empty());
    }

    @Test
    void shouldRequireUserToBeLoggedIn() {
      currentUser.setUser(null);
      controller =
          new MemoryTrackerController(
              modelFactoryService,
              makeMe.entityPersister,
              testabilitySettings,
              authorizationService,
              userService);
      assertThrows(ResponseStatusException.class, () -> controller.getRecentMemoryTrackers());
    }
  }

  @Nested
  class GetRecentlyReviewed {
    @Test
    void shouldReturnEmptyListWhenNoReviewed() {
      List<MemoryTracker> memoryTrackers = controller.getRecentlyReviewed();
      assertThat(memoryTrackers, empty());
    }

    @Test
    void shouldReturnRecentlyReviewedForCurrentUser() {
      MemoryTracker rp1 =
          makeMe.aMemoryTrackerFor(makeMe.aNote().please()).by(currentUser.getUser()).please();
      MemoryTracker rp2 =
          makeMe.aMemoryTrackerFor(makeMe.aNote().please()).by(currentUser.getUser()).please();

      // Mark as reviewed
      controller.markAsRepeated(rp1, true);
      controller.markAsRepeated(rp2, true);

      List<MemoryTracker> memoryTrackers = controller.getRecentlyReviewed();

      assertThat(memoryTrackers, hasSize(2));
      assertThat(memoryTrackers, containsInAnyOrder(rp1, rp2));
    }

    @Test
    void shouldRequireUserToBeLoggedIn() {
      currentUser.setUser(null);
      controller =
          new MemoryTrackerController(
              modelFactoryService,
              makeMe.entityPersister,
              testabilitySettings,
              authorizationService,
              userService);
      assertThrows(ResponseStatusException.class, () -> controller.getRecentlyReviewed());
    }
  }

  @Nested
  class answerSpellingQuestion {
    Note answerNote;
    MemoryTracker memoryTracker;
    AnswerSpellingDTO answerDTO = new AnswerSpellingDTO();

    @BeforeEach
    void setup() {
      answerNote = makeMe.aNote().rememberSpelling().please();
      memoryTracker =
          makeMe
              .aMemoryTrackerFor(answerNote)
              .by(currentUser.getUser())
              .forgettingCurveAndNextRecallAt(200)
              .spelling()
              .please();
      answerDTO.setSpellingAnswer(answerNote.getTopicConstructor());
    }

    @Test
    void answerOneOfTheTitles() {
      makeMe.theNote(answerNote).titleConstructor("this / that").please();
      answerDTO.setSpellingAnswer("this");
      assertTrue(controller.answerSpelling(memoryTracker, answerDTO).getIsCorrect());
      answerDTO.setSpellingAnswer("that");
      assertTrue(controller.answerSpelling(memoryTracker, answerDTO).getIsCorrect());
    }

    @Test
    void shouldValidateTheAnswerAndUpdateMemoryTracker() {
      Integer oldRepetitionCount = memoryTracker.getRepetitionCount();
      SpellingResultDTO answerResult = controller.answerSpelling(memoryTracker, answerDTO);
      assertTrue(answerResult.getIsCorrect());
      assertThat(memoryTracker.getRepetitionCount(), greaterThan(oldRepetitionCount));
    }

    @Test
    void shouldNoteIncreaseIndexIfRepeatImmediately() {
      testabilitySettings.timeTravelTo(memoryTracker.getLastRecalledAt());
      Integer oldForgettingCurveIndex = memoryTracker.getForgettingCurveIndex();
      controller.answerSpelling(memoryTracker, answerDTO);
      assertThat(memoryTracker.getForgettingCurveIndex(), equalTo(oldForgettingCurveIndex));
    }

    @Test
    void shouldIncreaseTheIndex() {
      testabilitySettings.timeTravelTo(memoryTracker.getNextRecallAt());
      Integer oldForgettingCurveIndex = memoryTracker.getForgettingCurveIndex();
      controller.answerSpelling(memoryTracker, answerDTO);
      assertThat(memoryTracker.getForgettingCurveIndex(), greaterThan(oldForgettingCurveIndex));
      assertThat(
          memoryTracker.getLastRecalledAt(), equalTo(testabilitySettings.getCurrentUTCTimestamp()));
    }

    @Test
    void shouldNotBeAbleToSeeNoteIDontHaveAccessTo() {
      AnswerSpellingDTO answer = new AnswerSpellingDTO();
      currentUser.setUser(null);
      controller =
          new MemoryTrackerController(
              modelFactoryService,
              makeMe.entityPersister,
              testabilitySettings,
              authorizationService,
              userService);
      assertThrows(
          ResponseStatusException.class, () -> controller.answerSpelling(memoryTracker, answer));
    }

    @Nested
    class WrongAnswer {
      @BeforeEach
      void setup() {
        answerDTO.setSpellingAnswer("wrong");
      }

      @Test
      void shouldValidateTheWrongAnswer() {
        testabilitySettings.timeTravelTo(memoryTracker.getNextRecallAt());
        Integer oldRepetitionCount = memoryTracker.getRepetitionCount();
        SpellingResultDTO answerResult = controller.answerSpelling(memoryTracker, answerDTO);
        assertFalse(answerResult.getIsCorrect());
        assertThat(memoryTracker.getRepetitionCount(), greaterThan(oldRepetitionCount));
      }

      @Test
      void shouldNotChangeTheLastRecalledAtTime() {
        testabilitySettings.timeTravelTo(memoryTracker.getNextRecallAt());
        Timestamp lastRecalledAt = memoryTracker.getLastRecalledAt();
        Integer oldForgettingCurveIndex = memoryTracker.getForgettingCurveIndex();
        controller.answerSpelling(memoryTracker, answerDTO);
        assertThat(memoryTracker.getForgettingCurveIndex(), lessThan(oldForgettingCurveIndex));
        assertThat(memoryTracker.getLastRecalledAt(), equalTo(lastRecalledAt));
      }

      @Test
      void shouldRepeatTheNextDay() {
        controller.answerSpelling(memoryTracker, answerDTO);
        assertThat(
            memoryTracker.getNextRecallAt(),
            lessThan(
                TimestampOperations.addHoursToTimestamp(
                    testabilitySettings.getCurrentUTCTimestamp(), 25)));
      }
    }
  }
}

package com.odde.doughnut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.odde.doughnut.controllers.dto.AnswerSpellingDTO;
import com.odde.doughnut.controllers.dto.SpellingResultDTO;
import com.odde.doughnut.entities.Answer;
import com.odde.doughnut.entities.MemoryTracker;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.QuestionType;
import com.odde.doughnut.entities.RecallPrompt;
import com.odde.doughnut.entities.User;
import com.odde.doughnut.exceptions.UnexpectedNoAccessRightException;
import com.odde.doughnut.utils.TimestampOperations;
import java.sql.Timestamp;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.server.ResponseStatusException;

class MemoryTrackerControllerTest extends ControllerTestBase {
  @Autowired MemoryTrackerController controller;

  @BeforeEach
  void setup() {
    currentUser.setUser(makeMe.aUser().please());
  }

  @Nested
  class GetSpellingQuestion {
    @Test
    void shouldReturnSpellingRecallPrompt() throws UnexpectedNoAccessRightException {
      Note note =
          makeMe
              .aNote("moon")
              .details("partner of earth")
              .creatorAndOwner(currentUser.getUser())
              .please();
      MemoryTracker memoryTracker = makeMe.aMemoryTrackerFor(note).please();

      RecallPrompt recallPrompt = controller.getSpellingQuestion(memoryTracker);
      assertThat(recallPrompt.getQuestionType(), equalTo(QuestionType.SPELLING));
      assertThat(recallPrompt.getMemoryTracker(), equalTo(memoryTracker));
      assertThat(recallPrompt.getPredefinedQuestion(), nullValue());
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

      @Test
      void reEnableShouldSetRemovedFromTrackingToFalse() throws UnexpectedNoAccessRightException {
        rp.setRemovedFromTracking(true);
        makeMe.entityPersister.merge(rp);
        controller.reEnable(rp);
        assertThat(rp.getRemovedFromTracking(), is(false));
      }

      @Test
      void shouldNotBeAbleToReEnableOthersMemoryTracker() {
        rp = makeMe.aMemoryTrackerBy(makeMe.aUser().please()).please();
        rp.setRemovedFromTracking(true);
        makeMe.entityPersister.merge(rp);
        assertThrows(UnexpectedNoAccessRightException.class, () -> controller.reEnable(rp));
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
      assertThrows(ResponseStatusException.class, () -> controller.getRecentMemoryTrackers());
    }

    @Test
    void shouldExcludeMemoryTrackersForDeletedNotes() {
      Note activeNote = makeMe.aNote().creatorAndOwner(currentUser.getUser()).please();
      Note deletedNote = makeMe.aNote().creatorAndOwner(currentUser.getUser()).please();
      MemoryTracker activeTracker =
          makeMe.aMemoryTrackerFor(activeNote).by(currentUser.getUser()).please();
      MemoryTracker deletedTracker =
          makeMe.aMemoryTrackerFor(deletedNote).by(currentUser.getUser()).please();

      deletedNote.setDeletedAt(testabilitySettings.getCurrentUTCTimestamp());
      makeMe.entityPersister.merge(deletedNote);

      List<MemoryTracker> memoryTrackers = controller.getRecentMemoryTrackers();

      assertThat(memoryTrackers, hasSize(1));
      assertThat(memoryTrackers, contains(activeTracker));
      assertThat(memoryTrackers, not(hasItem(deletedTracker)));
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
      assertThrows(ResponseStatusException.class, () -> controller.getRecentlyReviewed());
    }

    @Test
    void shouldExcludeMemoryTrackersForDeletedNotes() {
      Note activeNote = makeMe.aNote().creatorAndOwner(currentUser.getUser()).please();
      Note deletedNote = makeMe.aNote().creatorAndOwner(currentUser.getUser()).please();
      MemoryTracker activeTracker =
          makeMe.aMemoryTrackerFor(activeNote).by(currentUser.getUser()).please();
      MemoryTracker deletedTracker =
          makeMe.aMemoryTrackerFor(deletedNote).by(currentUser.getUser()).please();

      controller.markAsRepeated(activeTracker, true);
      controller.markAsRepeated(deletedTracker, true);

      deletedNote.setDeletedAt(testabilitySettings.getCurrentUTCTimestamp());
      makeMe.entityPersister.merge(deletedNote);

      List<MemoryTracker> memoryTrackers = controller.getRecentlyReviewed();

      assertThat(memoryTrackers, hasSize(1));
      assertThat(memoryTrackers, contains(activeTracker));
      assertThat(memoryTrackers, not(hasItem(deletedTracker)));
    }
  }

  @Nested
  class answerSpellingQuestion {
    Note answerNote;
    MemoryTracker memoryTracker;
    RecallPrompt recallPrompt;
    AnswerSpellingDTO answerDTO = new AnswerSpellingDTO();

    @BeforeEach
    void setup() throws UnexpectedNoAccessRightException {
      answerNote = makeMe.aNote().rememberSpelling().please();
      memoryTracker =
          makeMe
              .aMemoryTrackerFor(answerNote)
              .by(currentUser.getUser())
              .forgettingCurveAndNextRecallAt(200)
              .spelling()
              .please();
      recallPrompt = controller.getSpellingQuestion(memoryTracker);
      answerDTO.setSpellingAnswer(answerNote.getTopicConstructor());
      answerDTO.setRecallPromptId(recallPrompt.getId());
    }

    @Test
    void answerOneOfTheTitles() throws UnexpectedNoAccessRightException {
      makeMe.theNote(answerNote).titleConstructor("this / that").please();
      answerDTO.setSpellingAnswer("this");
      assertTrue(controller.answerSpelling(memoryTracker, answerDTO).getIsCorrect());
      // Create a new recall prompt for the second answer
      RecallPrompt secondRecallPrompt = controller.getSpellingQuestion(memoryTracker);
      AnswerSpellingDTO secondAnswerDTO = new AnswerSpellingDTO();
      secondAnswerDTO.setSpellingAnswer("that");
      secondAnswerDTO.setRecallPromptId(secondRecallPrompt.getId());
      assertTrue(controller.answerSpelling(memoryTracker, secondAnswerDTO).getIsCorrect());
    }

    @Test
    void shouldValidateTheAnswerAndUpdateMemoryTracker() throws UnexpectedNoAccessRightException {
      Integer oldRepetitionCount = memoryTracker.getRepetitionCount();
      SpellingResultDTO answerResult = controller.answerSpelling(memoryTracker, answerDTO);
      assertTrue(answerResult.getIsCorrect());
      assertThat(memoryTracker.getRepetitionCount(), greaterThan(oldRepetitionCount));
    }

    @Test
    void shouldAcceptThinkingTimeMs() throws UnexpectedNoAccessRightException {
      answerDTO.setThinkingTimeMs(5000);
      SpellingResultDTO answerResult = controller.answerSpelling(memoryTracker, answerDTO);
      assertTrue(answerResult.getIsCorrect());
      RecallPrompt reloadedPrompt = makeMe.refresh(recallPrompt);
      Answer answer = reloadedPrompt.getAnswer();
      assertNotNull(answer);
      assertThat(answer.getThinkingTimeMs(), equalTo(5000));
      assertThat(answer.getSpellingAnswer(), equalTo(answerDTO.getSpellingAnswer()));
      assertTrue(answer.getCorrect());
    }

    @Test
    void shouldCreateAnswerEntityForSpellingQuestion() throws UnexpectedNoAccessRightException {
      answerDTO.setSpellingAnswer(answerNote.getTopicConstructor());
      answerDTO.setThinkingTimeMs(3000);
      controller.answerSpelling(memoryTracker, answerDTO);

      RecallPrompt reloadedPrompt = makeMe.refresh(recallPrompt);
      Answer answer = reloadedPrompt.getAnswer();
      assertNotNull(answer);
      assertThat(answer.getSpellingAnswer(), equalTo(answerNote.getTopicConstructor()));
      assertThat(answer.getThinkingTimeMs(), equalTo(3000));
      assertTrue(answer.getCorrect());
    }

    @Test
    void shouldNotAllowAnsweringTwice() throws UnexpectedNoAccessRightException {
      controller.answerSpelling(memoryTracker, answerDTO);
      assertThrows(
          IllegalArgumentException.class,
          () -> controller.answerSpelling(memoryTracker, answerDTO));
    }

    @Test
    void shouldNoteIncreaseIndexIfRepeatImmediately() throws UnexpectedNoAccessRightException {
      testabilitySettings.timeTravelTo(memoryTracker.getLastRecalledAt());
      Integer oldForgettingCurveIndex = memoryTracker.getForgettingCurveIndex();
      controller.answerSpelling(memoryTracker, answerDTO);
      assertThat(memoryTracker.getForgettingCurveIndex(), equalTo(oldForgettingCurveIndex));
    }

    @Test
    void shouldIncreaseTheIndex() throws UnexpectedNoAccessRightException {
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
      answer.setRecallPromptId(recallPrompt.getId());
      currentUser.setUser(null);
      assertThrows(
          ResponseStatusException.class, () -> controller.answerSpelling(memoryTracker, answer));
    }

    @Test
    void shouldRequireRecallPromptId() {
      AnswerSpellingDTO answer = new AnswerSpellingDTO();
      answer.setSpellingAnswer(answerNote.getTopicConstructor());
      assertThrows(
          IllegalArgumentException.class, () -> controller.answerSpelling(memoryTracker, answer));
    }

    @Test
    void shouldValidateRecallPromptIsSpellingType() {
      RecallPrompt mcqPrompt = makeMe.aRecallPrompt().forMemoryTracker(memoryTracker).please();
      AnswerSpellingDTO answer = new AnswerSpellingDTO();
      answer.setSpellingAnswer(answerNote.getTopicConstructor());
      answer.setRecallPromptId(mcqPrompt.getId());
      assertThrows(
          IllegalArgumentException.class, () -> controller.answerSpelling(memoryTracker, answer));
    }

    @Nested
    class WrongAnswer {
      @BeforeEach
      void setup() {
        answerDTO.setSpellingAnswer("wrong");
      }

      @Test
      void shouldValidateTheWrongAnswer() throws UnexpectedNoAccessRightException {
        testabilitySettings.timeTravelTo(memoryTracker.getNextRecallAt());
        Integer oldRepetitionCount = memoryTracker.getRepetitionCount();
        SpellingResultDTO answerResult = controller.answerSpelling(memoryTracker, answerDTO);
        assertFalse(answerResult.getIsCorrect());
        assertThat(memoryTracker.getRepetitionCount(), greaterThan(oldRepetitionCount));
      }

      @Test
      void shouldNotChangeTheLastRecalledAtTime() throws UnexpectedNoAccessRightException {
        testabilitySettings.timeTravelTo(memoryTracker.getNextRecallAt());
        Timestamp lastRecalledAt = memoryTracker.getLastRecalledAt();
        Integer oldForgettingCurveIndex = memoryTracker.getForgettingCurveIndex();
        controller.answerSpelling(memoryTracker, answerDTO);
        assertThat(memoryTracker.getForgettingCurveIndex(), lessThan(oldForgettingCurveIndex));
        assertThat(memoryTracker.getLastRecalledAt(), equalTo(lastRecalledAt));
      }

      @Test
      void shouldRepeatTheNextDay() throws UnexpectedNoAccessRightException {
        controller.answerSpelling(memoryTracker, answerDTO);
        assertThat(
            memoryTracker.getNextRecallAt(),
            lessThan(
                TimestampOperations.addHoursToTimestamp(
                    testabilitySettings.getCurrentUTCTimestamp(), 25)));
      }
    }
  }

  @Nested
  class GetRecallPrompts {
    @Test
    void shouldReturnAllRecallPromptsOrderedByIdDesc() throws UnexpectedNoAccessRightException {
      Note note = makeMe.aNote().please();
      MemoryTracker memoryTracker =
          makeMe.aMemoryTrackerFor(note).by(currentUser.getUser()).please();

      RecallPrompt prompt1 =
          makeMe.aRecallPrompt().approvedQuestionOf(note).forMemoryTracker(memoryTracker).please();
      RecallPrompt prompt2 =
          makeMe.aRecallPrompt().approvedQuestionOf(note).forMemoryTracker(memoryTracker).please();
      RecallPrompt prompt3 =
          makeMe.aRecallPrompt().approvedQuestionOf(note).forMemoryTracker(memoryTracker).please();

      List<RecallPrompt> prompts = controller.getRecallPrompts(memoryTracker);

      assertThat(prompts, hasSize(3));
      assertThat(prompts.get(0).getId(), equalTo(prompt3.getId()));
      assertThat(prompts.get(1).getId(), equalTo(prompt2.getId()));
      assertThat(prompts.get(2).getId(), equalTo(prompt1.getId()));
    }

    @Test
    void shouldReturnEmptyListWhenNoPrompts() throws UnexpectedNoAccessRightException {
      Note note = makeMe.aNote().please();
      MemoryTracker memoryTracker =
          makeMe.aMemoryTrackerFor(note).by(currentUser.getUser()).please();

      List<RecallPrompt> prompts = controller.getRecallPrompts(memoryTracker);

      assertThat(prompts, empty());
    }

    @Test
    void shouldIncludeBothAnsweredAndUnansweredPrompts() throws UnexpectedNoAccessRightException {
      Note note = makeMe.aNote().please();
      MemoryTracker memoryTracker =
          makeMe.aMemoryTrackerFor(note).by(currentUser.getUser()).please();

      RecallPrompt unansweredPrompt =
          makeMe.aRecallPrompt().approvedQuestionOf(note).forMemoryTracker(memoryTracker).please();
      RecallPrompt answeredPrompt =
          makeMe
              .aRecallPrompt()
              .approvedQuestionOf(note)
              .forMemoryTracker(memoryTracker)
              .answerChoiceIndex(0)
              .please();

      List<RecallPrompt> prompts = controller.getRecallPrompts(memoryTracker);

      assertThat(prompts, hasSize(2));
      assertThat(prompts.get(0).getId(), equalTo(answeredPrompt.getId()));
      assertThat(prompts.get(1).getId(), equalTo(unansweredPrompt.getId()));
    }

    @Test
    void shouldNotBeAbleToGetRecallPromptsForOthersMemoryTracker() {
      MemoryTracker memoryTracker = makeMe.aMemoryTrackerBy(makeMe.aUser().please()).please();
      assertThrows(
          UnexpectedNoAccessRightException.class, () -> controller.getRecallPrompts(memoryTracker));
    }

    @Test
    void shouldRequireUserToBeLoggedIn() {
      currentUser.setUser(null);
      MemoryTracker memoryTracker = makeMe.aMemoryTrackerBy(makeMe.aUser().please()).please();
      assertThrows(ResponseStatusException.class, () -> controller.getRecallPrompts(memoryTracker));
    }
  }

  @Nested
  class DeleteUnansweredRecallPrompts {
    @Test
    void shouldDeleteAllUnansweredRecallPrompts() throws UnexpectedNoAccessRightException {
      Note note = makeMe.aNote().please();
      MemoryTracker memoryTracker =
          makeMe.aMemoryTrackerFor(note).by(currentUser.getUser()).please();

      RecallPrompt unansweredPrompt1 =
          makeMe.aRecallPrompt().approvedQuestionOf(note).forMemoryTracker(memoryTracker).please();
      RecallPrompt unansweredPrompt2 =
          makeMe.aRecallPrompt().approvedQuestionOf(note).forMemoryTracker(memoryTracker).please();
      RecallPrompt answeredPrompt =
          makeMe
              .aRecallPrompt()
              .approvedQuestionOf(note)
              .forMemoryTracker(memoryTracker)
              .answerChoiceIndex(0)
              .please();

      controller.deleteUnansweredRecallPrompts(memoryTracker);

      List<RecallPrompt> remainingPrompts = controller.getRecallPrompts(memoryTracker);
      assertThat(remainingPrompts, hasSize(1));
      assertThat(remainingPrompts.get(0).getId(), equalTo(answeredPrompt.getId()));
    }

    @Test
    void shouldDeleteNothingWhenAllPromptsAreAnswered() throws UnexpectedNoAccessRightException {
      Note note = makeMe.aNote().please();
      MemoryTracker memoryTracker =
          makeMe.aMemoryTrackerFor(note).by(currentUser.getUser()).please();

      RecallPrompt answeredPrompt1 =
          makeMe
              .aRecallPrompt()
              .approvedQuestionOf(note)
              .forMemoryTracker(memoryTracker)
              .answerChoiceIndex(0)
              .please();
      RecallPrompt answeredPrompt2 =
          makeMe
              .aRecallPrompt()
              .approvedQuestionOf(note)
              .forMemoryTracker(memoryTracker)
              .answerChoiceIndex(0)
              .please();

      controller.deleteUnansweredRecallPrompts(memoryTracker);

      List<RecallPrompt> remainingPrompts = controller.getRecallPrompts(memoryTracker);
      assertThat(remainingPrompts, hasSize(2));
      assertThat(remainingPrompts, containsInAnyOrder(answeredPrompt1, answeredPrompt2));
    }

    @Test
    void shouldDeleteNothingWhenNoPromptsExist() throws UnexpectedNoAccessRightException {
      Note note = makeMe.aNote().please();
      MemoryTracker memoryTracker =
          makeMe.aMemoryTrackerFor(note).by(currentUser.getUser()).please();

      controller.deleteUnansweredRecallPrompts(memoryTracker);

      List<RecallPrompt> remainingPrompts = controller.getRecallPrompts(memoryTracker);
      assertThat(remainingPrompts, empty());
    }

    @Test
    void shouldNotBeAbleToDeleteRecallPromptsForOthersMemoryTracker() {
      MemoryTracker memoryTracker = makeMe.aMemoryTrackerBy(makeMe.aUser().please()).please();
      assertThrows(
          UnexpectedNoAccessRightException.class,
          () -> controller.deleteUnansweredRecallPrompts(memoryTracker));
    }

    @Test
    void shouldRequireUserToBeLoggedIn() {
      currentUser.setUser(null);
      MemoryTracker memoryTracker = makeMe.aMemoryTrackerBy(makeMe.aUser().please()).please();
      assertThrows(
          ResponseStatusException.class,
          () -> controller.deleteUnansweredRecallPrompts(memoryTracker));
    }
  }
}

package com.odde.doughnut.services;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;

import com.odde.doughnut.controllers.dto.InitialInfo;
import com.odde.doughnut.entities.MemoryTracker;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.RecallPrompt;
import com.odde.doughnut.entities.User;
import com.odde.doughnut.testability.MakeMe;
import java.sql.Timestamp;
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
public class MemoryTrackerServiceTest {
  @Autowired MakeMe makeMe;
  @Autowired MemoryTrackerService memoryTrackerService;
  @Autowired UserService userService;

  @Autowired
  com.odde.doughnut.entities.repositories.MemoryTrackerRepository memoryTrackerRepository;

  User user;
  Timestamp day1;

  @BeforeEach
  void setup() {
    user = makeMe.aUser().please();
    day1 = makeMe.aTimestamp().of(1, 8).fromShanghai().please();
  }

  @Nested
  class Assimilating {

    @Test
    void assimilatingShouldSetBothInitialAndLastRecallAt() {
      Note note = makeMe.aNote().creatorAndOwner(user).please();
      InitialInfo initialInfo = new InitialInfo();
      initialInfo.noteId = note.getId();
      initialInfo.skipMemoryTracking = false;

      var memoryTrackers = memoryTrackerService.assimilate(initialInfo, user, day1);

      assertThat(memoryTrackers.get(0).getAssimilatedAt(), equalTo(day1));
      assertThat(memoryTrackers.get(0).getLastRecalledAt(), equalTo(day1));
    }
  }

  @Nested
  class GetAllRecallPrompts {
    @Test
    void shouldReturnEmptyListWhenNoPrompts() {
      Note note = makeMe.aNote().creatorAndOwner(user).please();
      MemoryTracker memoryTracker = makeMe.aMemoryTrackerFor(note).by(user).please();

      List<RecallPrompt> prompts = memoryTrackerService.getAllRecallPrompts(memoryTracker);

      assertThat(prompts, empty());
    }

    @Test
    void shouldReturnAllPromptsOrderedByIdDesc() {
      Note note = makeMe.aNote().creatorAndOwner(user).please();
      MemoryTracker memoryTracker = makeMe.aMemoryTrackerFor(note).by(user).please();

      RecallPrompt prompt1 =
          makeMe.aRecallPrompt().approvedQuestionOf(note).forMemoryTracker(memoryTracker).please();
      RecallPrompt prompt2 =
          makeMe.aRecallPrompt().approvedQuestionOf(note).forMemoryTracker(memoryTracker).please();
      RecallPrompt prompt3 =
          makeMe.aRecallPrompt().approvedQuestionOf(note).forMemoryTracker(memoryTracker).please();

      List<RecallPrompt> prompts = memoryTrackerService.getAllRecallPrompts(memoryTracker);

      assertThat(prompts, hasSize(3));
      assertThat(prompts.get(0).getId(), equalTo(prompt3.getId()));
      assertThat(prompts.get(1).getId(), equalTo(prompt2.getId()));
      assertThat(prompts.get(2).getId(), equalTo(prompt1.getId()));
    }

    @Test
    void shouldIncludeBothAnsweredAndUnansweredPrompts() {
      Note note = makeMe.aNote().creatorAndOwner(user).please();
      MemoryTracker memoryTracker = makeMe.aMemoryTrackerFor(note).by(user).please();

      RecallPrompt unansweredPrompt =
          makeMe.aRecallPrompt().approvedQuestionOf(note).forMemoryTracker(memoryTracker).please();
      RecallPrompt answeredPrompt =
          makeMe
              .aRecallPrompt()
              .approvedQuestionOf(note)
              .forMemoryTracker(memoryTracker)
              .answerChoiceIndex(0)
              .please();

      List<RecallPrompt> prompts = memoryTrackerService.getAllRecallPrompts(memoryTracker);

      assertThat(prompts, hasSize(2));
      assertThat(prompts.get(0).getId(), equalTo(answeredPrompt.getId()));
      assertThat(prompts.get(1).getId(), equalTo(unansweredPrompt.getId()));
    }
  }

  @Nested
  class CheckWrongAnswerThreshold {
    Note note;
    MemoryTracker memoryTracker;

    @BeforeEach
    void setup() {
      note = makeMe.aNote().creatorAndOwner(user).please();
      memoryTracker = makeMe.aMemoryTrackerFor(note).by(user).please();
    }

    @Test
    void shouldReturnFalseWhenBelowThreshold() {
      // Create 4 wrong answers (below default threshold of 5)
      for (int i = 0; i < 4; i++) {
        makeMe
            .aRecallPrompt()
            .approvedQuestionOf(note)
            .forMemoryTracker(memoryTracker)
            .answerChoiceIndex(1) // wrong
            .answerTimestamp(day1)
            .please();
      }

      boolean exceeded = memoryTrackerService.hasExceededWrongAnswerThreshold(note, day1, 14, 5);

      assertThat(exceeded, equalTo(false));
    }

    @Test
    void shouldReturnTrueWhenAtThreshold() {
      // Create 5 wrong answers (at default threshold of 5)
      for (int i = 0; i < 5; i++) {
        makeMe
            .aRecallPrompt()
            .approvedQuestionOf(note)
            .forMemoryTracker(memoryTracker)
            .answerChoiceIndex(1) // wrong
            .answerTimestamp(day1)
            .please();
      }

      boolean exceeded = memoryTrackerService.hasExceededWrongAnswerThreshold(note, day1, 14, 5);

      assertThat(exceeded, equalTo(true));
    }

    @Test
    void shouldReturnTrueWhenAboveThreshold() {
      // Create 6 wrong answers (above default threshold of 5)
      for (int i = 0; i < 6; i++) {
        makeMe
            .aRecallPrompt()
            .approvedQuestionOf(note)
            .forMemoryTracker(memoryTracker)
            .answerChoiceIndex(1) // wrong
            .answerTimestamp(day1)
            .please();
      }

      boolean exceeded = memoryTrackerService.hasExceededWrongAnswerThreshold(note, day1, 14, 5);

      assertThat(exceeded, equalTo(true));
    }
  }

  @Nested
  class MarkAsRepeatedWithThresholdCheck {
    Note note;
    MemoryTracker memoryTracker;

    @BeforeEach
    void setup() {
      note = makeMe.aNote().creatorAndOwner(user).please();
      memoryTracker = makeMe.aMemoryTrackerFor(note).by(user).please();
    }

    @Test
    void shouldReturnFalseWhenAnswerIsCorrect() {
      boolean thresholdExceeded =
          memoryTrackerService.markAsRepeated(day1, true, memoryTracker, 1000);

      assertThat(thresholdExceeded, equalTo(false));
    }

    @Test
    void shouldReturnFalseWhenWrongButBelowThreshold() {
      boolean thresholdExceeded =
          memoryTrackerService.markAsRepeated(day1, false, memoryTracker, 1000);

      assertThat(thresholdExceeded, equalTo(false));
    }

    @Test
    void shouldReturnTrueWhenWrongAndReachesThreshold() {
      // Create 5 previous wrong answers (at threshold)
      for (int i = 0; i < 5; i++) {
        makeMe
            .aRecallPrompt()
            .approvedQuestionOf(note)
            .forMemoryTracker(memoryTracker)
            .answerChoiceIndex(1)
            .answerTimestamp(day1)
            .please();
      }

      // This wrong answer should detect threshold is exceeded
      boolean thresholdExceeded =
          memoryTrackerService.markAsRepeated(day1, false, memoryTracker, 1000);

      assertThat(thresholdExceeded, equalTo(true));
    }
  }

  @Nested
  class ReAssimilate {
    Note note;
    MemoryTracker memoryTracker;

    @BeforeEach
    void setup() {
      note = makeMe.aNote().creatorAndOwner(user).please();
      memoryTracker = makeMe.aMemoryTrackerFor(note).by(user).please();
    }

    @Test
    void shouldSoftDeleteMemoryTrackerSoNoteReturnsToAssimilateState() {
      makeMe
          .aRecallPrompt()
          .approvedQuestionOf(note)
          .forMemoryTracker(memoryTracker)
          .answerChoiceIndex(1)
          .answerTimestamp(day1)
          .please();

      memoryTrackerService.reAssimilate(memoryTracker);

      List<MemoryTracker> trackers =
          memoryTrackerRepository.findByUserAndNote(user.getId(), note.getId());
      assertThat(trackers, empty());

      MemoryTracker refreshed =
          makeMe.entityPersister.find(MemoryTracker.class, memoryTracker.getId());
      assertThat(refreshed.getDeletedAt(), notNullValue());
    }

    @Test
    void shouldIncludeNoteInUnassimilatedCountAfterReAssimilate() {
      assertThat(userService.getUnassimilatedNoteCount(user), equalTo(0));

      memoryTrackerService.reAssimilate(memoryTracker);

      assertThat(userService.getUnassimilatedNoteCount(user), equalTo(1));
    }

    @Test
    void shouldExcludeSoftDeletedFromRecentAndRecalledLists() {
      assertThat(memoryTrackerService.findLast100ByUser(user.getId()), hasSize(1));
      assertThat(memoryTrackerService.findLast100RecalledByUser(user.getId()), hasSize(1));

      memoryTrackerService.reAssimilate(memoryTracker);

      assertThat(memoryTrackerService.findLast100ByUser(user.getId()), empty());
      assertThat(memoryTrackerService.findLast100RecalledByUser(user.getId()), empty());
    }

    @Test
    void markAsRepeatedShouldNotDeleteMemoryTracker() {
      // Create 5 previous wrong answers to exceed threshold
      for (int i = 0; i < 5; i++) {
        makeMe
            .aRecallPrompt()
            .approvedQuestionOf(note)
            .forMemoryTracker(memoryTracker)
            .answerChoiceIndex(1)
            .answerTimestamp(day1)
            .please();
      }

      boolean thresholdExceeded =
          memoryTrackerService.markAsRepeated(day1, false, memoryTracker, 1000);

      // markAsRepeated should return true but NOT delete the MemoryTracker
      assertThat(thresholdExceeded, equalTo(true));
      List<MemoryTracker> trackers =
          memoryTrackerRepository.findByUserAndNote(user.getId(), note.getId());
      assertThat(trackers, hasSize(1));
    }
  }
}

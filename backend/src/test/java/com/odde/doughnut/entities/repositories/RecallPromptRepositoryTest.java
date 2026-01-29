package com.odde.doughnut.entities.repositories;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import com.odde.doughnut.entities.MemoryTracker;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.User;
import com.odde.doughnut.testability.MakeMe;
import java.sql.Timestamp;
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
public class RecallPromptRepositoryTest {
  @Autowired MakeMe makeMe;
  @Autowired RecallPromptRepository recallPromptRepository;
  User user;
  Note note;
  MemoryTracker memoryTracker;
  Timestamp twoWeeksAgo;
  Timestamp now;

  @BeforeEach
  void setup() {
    user = makeMe.aUser().please();
    note = makeMe.aNote().creatorAndOwner(user).please();
    memoryTracker = makeMe.aMemoryTrackerFor(note).by(user).please();
    now = makeMe.aTimestamp().of(15, 8).fromShanghai().please();
    twoWeeksAgo = makeMe.aTimestamp().of(1, 8).fromShanghai().please();
  }

  @Nested
  class CountWrongAnswersSince {
    @Test
    void shouldReturnZeroWhenNoAnswers() {
      int count = recallPromptRepository.countWrongAnswersSince(note.getId(), twoWeeksAgo);
      assertThat(count, equalTo(0));
    }

    @Test
    void shouldCountWrongAnswersOnly() {
      // correctAnswerIndex defaults to 0, so answerChoiceIndex(1) = wrong
      makeMe
          .aRecallPrompt()
          .approvedQuestionOf(note)
          .forMemoryTracker(memoryTracker)
          .answerChoiceIndex(1) // wrong answer
          .answerTimestamp(now)
          .please();

      int count = recallPromptRepository.countWrongAnswersSince(note.getId(), twoWeeksAgo);
      assertThat(count, equalTo(1));
    }

    @Test
    void shouldNotCountCorrectAnswers() {
      makeMe
          .aRecallPrompt()
          .approvedQuestionOf(note)
          .forMemoryTracker(memoryTracker)
          .answerChoiceIndex(0) // correct answer
          .answerTimestamp(now)
          .please();

      int count = recallPromptRepository.countWrongAnswersSince(note.getId(), twoWeeksAgo);
      assertThat(count, equalTo(0));
    }

    @Test
    void shouldNotCountAnswersBeforeSinceTimestamp() {
      Timestamp beforePeriod = makeMe.aTimestamp().of(0, 8).fromShanghai().please();
      makeMe
          .aRecallPrompt()
          .approvedQuestionOf(note)
          .forMemoryTracker(memoryTracker)
          .answerChoiceIndex(1) // wrong answer
          .answerTimestamp(beforePeriod)
          .please();

      int count = recallPromptRepository.countWrongAnswersSince(note.getId(), twoWeeksAgo);
      assertThat(count, equalTo(0));
    }

    @Test
    void shouldCountMultipleWrongAnswers() {
      Timestamp day5 = makeMe.aTimestamp().of(5, 8).fromShanghai().please();
      Timestamp day10 = makeMe.aTimestamp().of(10, 8).fromShanghai().please();

      makeMe
          .aRecallPrompt()
          .approvedQuestionOf(note)
          .forMemoryTracker(memoryTracker)
          .answerChoiceIndex(1)
          .answerTimestamp(day5)
          .please();
      makeMe
          .aRecallPrompt()
          .approvedQuestionOf(note)
          .forMemoryTracker(memoryTracker)
          .answerChoiceIndex(1)
          .answerTimestamp(day10)
          .please();

      int count = recallPromptRepository.countWrongAnswersSince(note.getId(), twoWeeksAgo);
      assertThat(count, equalTo(2));
    }

    @Test
    void shouldOnlyCountAnswersForSpecificNote() {
      Note otherNote = makeMe.aNote().creatorAndOwner(user).please();
      MemoryTracker otherTracker = makeMe.aMemoryTrackerFor(otherNote).by(user).please();

      makeMe
          .aRecallPrompt()
          .approvedQuestionOf(note)
          .forMemoryTracker(memoryTracker)
          .answerChoiceIndex(1)
          .answerTimestamp(now)
          .please();
      makeMe
          .aRecallPrompt()
          .approvedQuestionOf(otherNote)
          .forMemoryTracker(otherTracker)
          .answerChoiceIndex(1)
          .answerTimestamp(now)
          .please();

      int count = recallPromptRepository.countWrongAnswersSince(note.getId(), twoWeeksAgo);
      assertThat(count, equalTo(1));
    }
  }
}

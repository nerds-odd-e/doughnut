package com.odde.doughnut.entities;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;

import com.odde.doughnut.controllers.dto.SpellingQuestion;
import com.odde.doughnut.testability.MakeMe;
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
class RecallPromptTest {
  @Autowired MakeMe makeMe;
  Note note;
  MemoryTracker memoryTracker;

  @BeforeEach
  void setup() {
    note = makeMe.aNote("sedition").details("Sedition means incite violence").please();
    memoryTracker = makeMe.aMemoryTrackerFor(note).by(makeMe.aUser().please()).please();
  }

  @Nested
  class GetSpellingQuestion {
    @Test
    void shouldReturnSpellingQuestionWhenQuestionTypeIsSpelling() {
      RecallPrompt recallPrompt = new RecallPrompt();
      recallPrompt.setQuestionType(QuestionType.SPELLING);
      recallPrompt.setMemoryTracker(memoryTracker);

      SpellingQuestion spellingQuestion = recallPrompt.getSpellingQuestion();

      assertThat(spellingQuestion, org.hamcrest.Matchers.notNullValue());
      assertThat(
          spellingQuestion.getStem(),
          org.hamcrest.Matchers.containsString("means incite violence"));
      assertThat(spellingQuestion.getNotebook(), equalTo(note.getNotebook()));
    }

    @Test
    void shouldReturnNullWhenQuestionTypeIsNotSpelling() {
      RecallPrompt recallPrompt = new RecallPrompt();
      recallPrompt.setQuestionType(QuestionType.MCQ);
      recallPrompt.setMemoryTracker(memoryTracker);

      SpellingQuestion spellingQuestion = recallPrompt.getSpellingQuestion();

      assertThat(spellingQuestion, nullValue());
    }
  }

  @Nested
  class GetQuestionGeneratedTime {
    @Test
    void shouldReturnRecallPromptCreatedAtForMCQ() {
      java.sql.Timestamp createdAt = new java.sql.Timestamp(System.currentTimeMillis());
      RecallPrompt recallPrompt = new RecallPrompt();
      recallPrompt.setQuestionType(QuestionType.MCQ);
      recallPrompt.setMemoryTracker(memoryTracker);
      recallPrompt.setCreatedAt(createdAt);
      recallPrompt.setPredefinedQuestion(
          makeMe.aPredefinedQuestion().ofAIGeneratedQuestionForNote(note).please());

      java.sql.Timestamp questionGeneratedTime = recallPrompt.getQuestionGeneratedTime();

      assertThat(questionGeneratedTime, equalTo(createdAt));
    }

    @Test
    void shouldReturnRecallPromptCreatedAtForSpelling() {
      java.sql.Timestamp createdAt = new java.sql.Timestamp(System.currentTimeMillis());
      RecallPrompt recallPrompt = new RecallPrompt();
      recallPrompt.setQuestionType(QuestionType.SPELLING);
      recallPrompt.setMemoryTracker(memoryTracker);
      recallPrompt.setCreatedAt(createdAt);

      java.sql.Timestamp questionGeneratedTime = recallPrompt.getQuestionGeneratedTime();

      assertThat(questionGeneratedTime, equalTo(createdAt));
    }
  }
}

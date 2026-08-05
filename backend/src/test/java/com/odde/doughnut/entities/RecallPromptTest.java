package com.odde.doughnut.entities;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;
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
  User user;
  Note note;
  MemoryTracker memoryTracker;

  @BeforeEach
  void setup() {
    user = makeMe.aUser().please();
    note =
        makeMe
            .aNote("sedition")
            .notebookOwnedBy(user)
            .content("Sedition means incite violence")
            .please();
    memoryTracker = makeMe.aMemoryTrackerFor(note).please();
  }

  private RecallPrompt spellingPromptFor(MemoryTracker tracker) {
    RecallPrompt recallPrompt = new RecallPrompt();
    recallPrompt.setQuestionType(QuestionType.SPELLING);
    recallPrompt.setMemoryTracker(tracker);
    return recallPrompt;
  }

  @Nested
  class GetNotebook {
    @Test
    void shouldReturnNotebookFromMemoryTrackerWhenSpellingAndNoPredefinedQuestion() {
      assertThat(spellingPromptFor(memoryTracker).getNotebook(), equalTo(note.getNotebook()));
    }
  }

  @Nested
  class GetSpellingQuestion {
    @Test
    void shouldReturnSpellingQuestionWhenQuestionTypeIsSpelling() {
      SpellingQuestion spellingQuestion = spellingPromptFor(memoryTracker).getSpellingQuestion();

      assertThat(spellingQuestion.getStem(), containsString("means incite violence"));
      assertThat(spellingQuestion.getNotebook(), equalTo(note.getNotebook()));
    }

    @Test
    void spellingStemOmitsLeadingYamlFrontmatterFence() {
      Note noteWithFm =
          makeMe
              .aNote("sedition")
              .notebookOwnedBy(user)
              .content(
                  "---\n" + "see: \"[[Other]]\"\n" + "---\n" + "Sedition means incite violence")
              .please();
      SpellingQuestion spellingQuestion =
          spellingPromptFor(makeMe.aMemoryTrackerFor(noteWithFm).please()).getSpellingQuestion();

      assertThat(spellingQuestion.getStem(), not(containsString("see:")));
      assertThat(spellingQuestion.getStem(), not(containsString("---")));
    }

    @Test
    void shouldReturnNullWhenQuestionTypeIsNotSpelling() {
      RecallPrompt recallPrompt = new RecallPrompt();
      recallPrompt.setQuestionType(QuestionType.MCQ);
      recallPrompt.setMemoryTracker(memoryTracker);

      assertThat(recallPrompt.getSpellingQuestion(), nullValue());
    }
  }
}

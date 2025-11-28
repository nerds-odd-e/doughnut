package com.odde.doughnut.testability.builders;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.odde.doughnut.entities.AnsweredQuestion;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.RecallPrompt;
import com.odde.doughnut.services.ai.MCQWithAnswer;
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
class AIGeneratedQuizFactoryTest {

  @Autowired MakeMe makeMe;
  Note note;
  MCQWithAnswer mcqWithAnswer;

  @BeforeEach
  void setup() {
    note = makeMe.aNote("saying").details("Rome is not built in a day").please();
    mcqWithAnswer =
        makeMe
            .aMCQWithAnswer()
            .stem("How long did it take to build Rome?")
            .choices("1/2 day", "1 day", "more than 1 day")
            .correctChoiceIndex(2)
            .please();
  }

  @Test
  void shouldIncludeQuestionStem() {
    RecallPrompt recallPrompt = buildQuestion();
    assertThat(
        recallPrompt.getMultipleChoicesQuestion().getF0__stem(),
        containsString("How long did it take to build Rome?"));
  }

  @Nested
  class Answer {

    @Test
    void wrong() {
      AnsweredQuestion answerResult =
          questionBuilder().answerChoiceIndex(0).please(false).getAnsweredQuestion();
      assertFalse(answerResult.answer.getCorrect());
    }

    @Test
    void correct() {
      AnsweredQuestion answerResult =
          questionBuilder()
              .answerChoiceIndex(mcqWithAnswer.getF1__correctChoiceIndex())
              .please(false)
              .getAnsweredQuestion();
      assertTrue(answerResult.answer.getCorrect());
    }
  }

  private RecallPromptBuilder questionBuilder() {
    return makeMe.aRecallPrompt().ofAIGeneratedQuestion(mcqWithAnswer, note);
  }

  private RecallPrompt buildQuestion() {
    return questionBuilder().inMemoryPlease();
  }
}

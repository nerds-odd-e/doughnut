package com.odde.doughnut.models.quizFacotries;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.odde.doughnut.controllers.json.QuizQuestion;
import com.odde.doughnut.entities.AnsweredQuestion;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.models.UserModel;
import com.odde.doughnut.services.ai.MCQWithAnswer;
import com.odde.doughnut.testability.MakeMe;
import com.odde.doughnut.testability.builders.QuizQuestionBuilder;
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
  UserModel userModel;
  Note note;
  MCQWithAnswer mcqWithAnswer;

  @BeforeEach
  void setup() {
    userModel = makeMe.aUser().toModelPlease();
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
    assertThat(buildQuestion().getStem(), containsString("How long did it take to build Rome?"));
  }

  @Nested
  class Answer {

    @Test
    void wrong() {
      AnsweredQuestion answerResult =
          makeMe
              .anAnswerViewedByUser()
              .forQuestion(questionBuilder().inMemoryPlease())
              .choiceIndex(0)
              .inMemoryPlease();
      assertFalse(answerResult.correct);
    }

    @Test
    void correct() {
      AnsweredQuestion answerResult =
          makeMe
              .anAnswerViewedByUser()
              .forQuestion(questionBuilder().inMemoryPlease())
              .choiceIndex(mcqWithAnswer.correctChoiceIndex)
              .inMemoryPlease();
      assertTrue(answerResult.correct);
    }
  }

  private QuizQuestionBuilder questionBuilder() {
    return makeMe.aQuestion().ofAIGeneratedQuestion(mcqWithAnswer, note.getThing());
  }

  private QuizQuestion buildQuestion() {
    return questionBuilder().ViewedByUserPlease();
  }
}

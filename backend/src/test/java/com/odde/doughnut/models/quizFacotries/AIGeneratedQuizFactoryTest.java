package com.odde.doughnut.models.quizFacotries;

import static com.odde.doughnut.entities.QuizQuestionEntity.QuestionType.AI_QUESTION;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.odde.doughnut.entities.AnswerViewedByUser;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.ReviewPoint;
import com.odde.doughnut.entities.json.AIGeneratedQuestion;
import com.odde.doughnut.entities.json.QuizQuestion;
import com.odde.doughnut.models.UserModel;
import com.odde.doughnut.testability.MakeMe;
import com.odde.doughnut.testability.builders.QuizQuestionBuilder;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = {"classpath:repository.xml"})
@Transactional
class AIGeneratedQuizFactoryTest {

  @Autowired MakeMe makeMe;
  UserModel userModel;
  Note note;
  ReviewPoint reviewPoint;
  AIGeneratedQuestion aiGeneratedQuestion = new AIGeneratedQuestion();

  @BeforeEach
  void setup() {
    userModel = makeMe.aUser().toModelPlease();
    note =
        makeMe
            .aNote("saying")
            .description("Rome is not built in a day")
            .asHeadNoteOfANotebook()
            .please();
    reviewPoint = makeMe.aReviewPointFor(note).by(userModel).inMemoryPlease();
    aiGeneratedQuestion.stem = "How long did it take to build Rome?";
    aiGeneratedQuestion.correctChoice = "more than 1 day";
    aiGeneratedQuestion.incorrectChoices = List.of("1 day", "1/2 day");
  }

  @Test
  void shouldIncludeQuestionStem() {
    assertThat(
        buildQuestion().getDescription(), containsString("How long did it take to build Rome?"));
  }

  @Nested
  class Answer {

    @Test
    void wrong() {
      AnswerViewedByUser answerResult =
          makeMe
              .anAnswerViewedByUser()
              .forQuestion(questionBuilder().inMemoryPlease())
              .answerWithSpelling("yes")
              .inMemoryPlease();
      assertFalse(answerResult.correct);
    }

    @Test
    void correct() {
      AnswerViewedByUser answerResult =
          makeMe
              .anAnswerViewedByUser()
              .forQuestion(questionBuilder().inMemoryPlease())
              .answerWithSpelling("more than 1 day")
              .inMemoryPlease();
      assertTrue(answerResult.correct);
    }
  }

  private QuizQuestionBuilder questionBuilder() {
    return makeMe.aQuestion().of(AI_QUESTION, reviewPoint).aiQuestion(aiGeneratedQuestion);
  }

  private QuizQuestion buildQuestion() {
    return questionBuilder().ViewedByUserPlease();
  }
}

package com.odde.doughnut.entities;

import static com.odde.doughnut.entities.QuizQuestionEntity.QuestionType.SPELLING;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.odde.doughnut.testability.MakeMe;
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
class AnswerViewedByUserTest {
  @Autowired MakeMe makeMe;

  @Nested
  class ClozeSelectionQuestion {
    Note note;
    ReviewPoint reviewPoint;

    @BeforeEach
    void setup() {
      User user = makeMe.aUser().please();
      Note top = makeMe.aNote().creatorAndOwner(user).please();
      note =
          makeMe
              .aNote("this / that")
              .description("description")
              .under(top)
              .rememberSpelling()
              .please();
      makeMe.aNote().under(top).please();
      reviewPoint = makeMe.aReviewPointFor(note).by(user).please();
      makeMe.refresh(top);
    }

    @Test
    void correct() {
      AnswerViewedByUser answer =
          makeMe
              .anAnswerViewedByUser()
              .validQuestionOfType(SPELLING, reviewPoint)
              .answerWithSpelling("this")
              .inMemoryPlease();
      assertTrue(answer.answerResult.correct);
    }

    @Test
    void correctWhenThereAreExtraSpace() {
      AnswerViewedByUser answer =
          makeMe
              .anAnswerViewedByUser()
              .validQuestionOfType(SPELLING, reviewPoint)
              .answerWithSpelling("this ")
              .inMemoryPlease();
      assertTrue(answer.answerResult.correct);
    }

    @Test
    void literalAnswer() {
      AnswerViewedByUser answerResult =
          makeMe
              .anAnswerViewedByUser()
              .validQuestionOfType(SPELLING, reviewPoint)
              .answerWithSpelling("this / that")
              .inMemoryPlease();
      assertTrue(answerResult.answerResult.correct);
    }
  }
}

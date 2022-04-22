package com.odde.doughnut.entities;

import static com.odde.doughnut.entities.QuizQuestion.QuestionType.CLOZE_SELECTION;
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
      note = makeMe.aNote("this / that").inMemoryPlease();
      reviewPoint = makeMe.aReviewPointFor(note).inMemoryPlease();
    }

    @Test
    void correct() {
      AnswerViewedByUser answer =
          makeMe.anAnswerFor(reviewPoint).type(CLOZE_SELECTION).answer("this").inMemoryPlease();
      assertTrue(answer.correct);
    }

    @Test
    void literalAnswer() {
      AnswerViewedByUser answerResult =
          makeMe
              .anAnswerFor(reviewPoint)
              .type(CLOZE_SELECTION)
              .answer("this / that")
              .inMemoryPlease();
      assertTrue(answerResult.correct);
    }
  }
}

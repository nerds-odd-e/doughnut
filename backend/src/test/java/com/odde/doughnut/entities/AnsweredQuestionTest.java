package com.odde.doughnut.entities;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.odde.doughnut.factoryServices.quizFacotries.factories.SpellingQuizFactory;
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
class AnsweredQuestionTest {
  @Autowired MakeMe makeMe;

  @Nested
  class ClozeSelectionQuestion {
    Note note;
    SpellingQuizFactory spellingQuizFactory;

    @BeforeEach
    void setup() {
      User user = makeMe.aUser().please();
      Note top = makeMe.aNote().creatorAndOwner(user).please();
      note =
          makeMe.aNote("this / that").details("description").under(top).rememberSpelling().please();
      makeMe.aNote().under(top).please();
      spellingQuizFactory = new SpellingQuizFactory(note);
    }

    @Test
    void correct() {
      AnsweredQuestion answer =
          makeMe
              .anAnswerViewedByUser()
              .validQuestionOfType(spellingQuizFactory)
              .answerWithSpelling("this")
              .inMemoryPlease();
      assertTrue(answer.correct);
    }

    @Test
    void correctWhenThereAreExtraSpace() {
      AnsweredQuestion answer =
          makeMe
              .anAnswerViewedByUser()
              .validQuestionOfType(spellingQuizFactory)
              .answerWithSpelling("this ")
              .inMemoryPlease();
      assertTrue(answer.correct);
    }

    @Test
    void literalAnswer() {
      AnsweredQuestion answerResult =
          makeMe
              .anAnswerViewedByUser()
              .validQuestionOfType(spellingQuizFactory)
              .answerWithSpelling("this / that")
              .inMemoryPlease();
      assertTrue(answerResult.correct);
    }
  }
}

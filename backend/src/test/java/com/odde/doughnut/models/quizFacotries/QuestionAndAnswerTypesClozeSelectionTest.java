package com.odde.doughnut.models.quizFacotries;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.QuestionAndAnswer;
import com.odde.doughnut.factoryServices.quizFacotries.QuizQuestionNotPossibleException;
import com.odde.doughnut.factoryServices.quizFacotries.QuizQuestionServant;
import com.odde.doughnut.factoryServices.quizFacotries.factories.ClozeTitleSelectionQuizFactory;
import com.odde.doughnut.models.randomizers.NonRandomizer;
import com.odde.doughnut.testability.MakeMe;
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
class QuestionAndAnswerTypesClozeSelectionTest {
  @Autowired MakeMe makeMe;

  @Nested
  class ClozeQuestion {
    Note top;
    Note note1;
    Note note2;

    @BeforeEach
    void setup() {
      top = makeMe.aNote().please();
      note1 = makeMe.aNote("target").under(top).please();
      note2 = makeMe.aNote("source").under(top).please();
    }

    @Test
    void NotForNoteWithoutVisibleDescription() {
      makeMe.theNote(note1).details("<p>  <br>  <br/>  </p>  <br>").please();
      QuestionAndAnswer questionAndAnswer = buildClozeQuizQuestion();
      assertThat(questionAndAnswer, nullValue());
    }

    @Test
    void shouldIncludeRightAnswers() {
      QuestionAndAnswer questionAndAnswer = buildClozeQuizQuestion();
      assertThat(
          questionAndAnswer.getMultipleChoicesQuestion().getStem(), containsString("descrption"));
      List<String> options = questionAndAnswer.getMultipleChoicesQuestion().getChoices();
      assertThat(note2.getTopicConstructor(), in(options));
      assertThat(note1.getTopicConstructor(), in(options));
    }

    private QuestionAndAnswer buildClozeQuizQuestion() {
      QuizQuestionServant servant =
          new QuizQuestionServant(null, new NonRandomizer(), makeMe.modelFactoryService);
      try {
        return new ClozeTitleSelectionQuizFactory(note1, servant).buildValidQuizQuestion();
      } catch (QuizQuestionNotPossibleException e) {
        return null;
      }
    }
  }
}

package com.odde.doughnut.models.quizFacotries;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.PredefinedQuestion;
import com.odde.doughnut.factoryServices.quizFacotries.PredefinedQuestionNotPossibleException;
import com.odde.doughnut.factoryServices.quizFacotries.PredefinedQuestionServant;
import com.odde.doughnut.factoryServices.quizFacotries.factories.ClozeTitleSelectionPredefinedFactory;
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
class PredefinedQuestionTypesClozeSelectionTest {
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
      PredefinedQuestion predefinedQuestion = buildClozeQuizQuestion();
      assertThat(predefinedQuestion, nullValue());
    }

    @Test
    void shouldIncludeRightAnswers() {
      PredefinedQuestion predefinedQuestion = buildClozeQuizQuestion();
      assertThat(
          predefinedQuestion.getQuizQuestion1().getMultipleChoicesQuestion().getStem(),
          containsString("descrption"));
      List<String> options =
          predefinedQuestion.getQuizQuestion1().getMultipleChoicesQuestion().getChoices();
      assertThat(note2.getTopicConstructor(), in(options));
      assertThat(note1.getTopicConstructor(), in(options));
    }

    private PredefinedQuestion buildClozeQuizQuestion() {
      PredefinedQuestionServant servant =
          new PredefinedQuestionServant(null, new NonRandomizer(), makeMe.modelFactoryService);
      try {
        return new ClozeTitleSelectionPredefinedFactory(note1, servant)
            .buildValidPredefinedQuestion();
      } catch (PredefinedQuestionNotPossibleException e) {
        return null;
      }
    }
  }
}

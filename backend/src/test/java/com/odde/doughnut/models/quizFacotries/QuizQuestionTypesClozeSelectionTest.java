package com.odde.doughnut.models.quizFacotries;

import static com.odde.doughnut.entities.QuizQuestionEntity.QuestionType.CLOZE_SELECTION;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.ReviewPoint;
import com.odde.doughnut.entities.json.QuizQuestion;
import com.odde.doughnut.testability.MakeMe;
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
class QuizQuestionTypesClozeSelectionTest {
  @Autowired MakeMe makeMe;

  @Nested
  class ClozeQuestion {
    Note top;
    Note note1;
    Note note2;
    ReviewPoint reviewPoint;

    @BeforeEach
    void setup() {
      top = makeMe.aHeadNote().please();
      note1 = makeMe.aNote("target").under(top).please();
      note2 = makeMe.aNote("source").under(top).please();
      reviewPoint = makeMe.aReviewPointFor(note1).inMemoryPlease();
    }

    @Test
    void NotForNoteWithoutVisibleDescription() {
      makeMe.theNote(note1).description("<p>  <br>  <br/>  </p>  <br>").please();
      makeMe.refresh(top);
      QuizQuestion quizQuestion = buildClozeQuizQuestion();
      assertThat(quizQuestion, nullValue());
    }

    @Test
    void shouldIncludeRightAnswers() {
      makeMe.refresh(top);
      QuizQuestion quizQuestion = buildClozeQuizQuestion();
      assertThat(quizQuestion.getStem(), equalTo("descrption"));
      assertThat(quizQuestion.getMainTopic(), equalTo(""));
      List<String> options = toOptionStrings(quizQuestion);
      assertThat(note2.getTitle(), in(options));
      assertThat(note1.getTitle(), in(options));
    }

    private QuizQuestion buildClozeQuizQuestion() {
      return makeMe.buildAQuestion(CLOZE_SELECTION, reviewPoint);
    }

    private List<String> toOptionStrings(QuizQuestion quizQuestion) {
      return quizQuestion.getChoices().stream().map(QuizQuestion.Choice::getDisplay).toList();
    }
  }
}

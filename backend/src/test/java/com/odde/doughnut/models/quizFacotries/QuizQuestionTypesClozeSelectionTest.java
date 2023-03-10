package com.odde.doughnut.models.quizFacotries;

import static com.odde.doughnut.entities.QuizQuestion.QuestionType.CLOZE_SELECTION;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import com.odde.doughnut.entities.Link;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.ReviewPoint;
import com.odde.doughnut.entities.json.LinksOfANote;
import com.odde.doughnut.entities.json.QuizQuestionViewedByUser;
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
      QuizQuestionViewedByUser quizQuestion = buildClozeQuizQuestion();
      assertThat(quizQuestion, nullValue());
    }

    @Test
    void shouldIncludeRightAnswers() {
      makeMe.refresh(top);
      QuizQuestionViewedByUser quizQuestion = buildClozeQuizQuestion();
      assertThat(quizQuestion.getDescription(), equalTo("descrption"));
      assertThat(quizQuestion.getMainTopic(), equalTo(""));
      List<String> options = toOptionStrings(quizQuestion);
      assertThat(note2.getTitle(), in(options));
      assertThat(note1.getTitle(), in(options));
    }

    @Test
    void shouldIncludeOpenLinks() {
      makeMe.theNote(note1).linkTo(note2, Link.LinkType.TAGGED_BY).please();
      makeMe.theNote(note1).linkTo(note2, Link.LinkType.SPECIALIZE).please();
      makeMe.refresh(top);
      QuizQuestionViewedByUser quizQuestion = buildClozeQuizQuestion();
      LinksOfANote hintLinks = quizQuestion.getHintLinks();
      assertThat(Link.LinkType.TAGGED_BY, in(hintLinks.getLinks().keySet()));
      assertThat(Link.LinkType.SPECIALIZE, not(in(hintLinks.getLinks().keySet())));
    }

    private QuizQuestionViewedByUser buildClozeQuizQuestion() {
      return makeMe.buildAQuestion(CLOZE_SELECTION, reviewPoint);
    }

    private List<String> toOptionStrings(QuizQuestionViewedByUser quizQuestion) {
      return quizQuestion.getOptions().stream()
          .map(QuizQuestionViewedByUser.Option::getDisplay)
          .toList();
    }
  }
}

package com.odde.doughnut.models.quizFacotries;

import static com.odde.doughnut.entities.QuizQuestionEntity.QuestionType.DESCRIPTION_LINK_TARGET;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.odde.doughnut.entities.AnswerViewedByUser;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.ReviewPoint;
import com.odde.doughnut.entities.json.QuizQuestion;
import com.odde.doughnut.models.UserModel;
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
class DescriptionLinkTargetQuizFactoryTest {

  @Autowired MakeMe makeMe;
  UserModel userModel;
  Note top;
  Note target;
  Note source;
  Note anotherSource;
  ReviewPoint reviewPoint;

  @BeforeEach
  void setup() {
    userModel = makeMe.aUser().toModelPlease();
    top = makeMe.aNote().creatorAndOwner(userModel).please();
    target = makeMe.aNote("rome").under(top).please();
    source =
        makeMe
            .aNote("saying")
            .description("Rome is not built in a day")
            .under(top)
            .linkTo(target)
            .please();
    reviewPoint = makeMe.aReviewPointFor(source.getLinks().get(0)).by(userModel).inMemoryPlease();
    anotherSource = makeMe.aNote("pompeii").under(top).please();
    makeMe.refresh(top);
  }

  @Nested
  class WhenTheNoteDoesnotHaveDescription {
    @BeforeEach
    void setup() {
      makeMe.theNote(source).description("").please();
    }

    @Test
    void shouldBeInvalid() {
      assertThat(buildQuestion(), nullValue());
    }
  }

  @Test
  void shouldIncludeRightAnswers() {
    assertThat(
        buildQuestion().getDescription(),
        containsString(
            "<p>The following descriptions is a specialization of:</p><pre style='white-space: pre-wrap;'><mark title='Hidden text that is matching the answer'>[...]</mark> is not built in a day</pre>"));
  }

  @Test
  void shouldIncludeMasks() {
    makeMe.theNote(source).title("token").description("token /.").please();
    assertThat(
        buildQuestion().getDescription(),
        containsString("<mark title='Hidden text that is matching the answer'>[...]</mark> /."));
  }

  @Nested
  class WhenThereIsReviewPointForTheSourceNote {
    ReviewPoint sourceReviewPoint;

    @BeforeEach
    void setup() {
      sourceReviewPoint = makeMe.aReviewPointFor(source).by(userModel).please();
      makeMe.refresh(userModel.getEntity());
    }

    @Test
    void shouldIncludeRightAnswers() {
      assertThat(buildQuestion().getViceReviewPointIdList(), contains(sourceReviewPoint.getId()));
    }
  }

  @Nested
  class Answer {
    @Test
    void correct() {
      AnswerViewedByUser answerResult =
          makeMe
              .anAnswerViewedByUser()
              .validQuestionOfType(DESCRIPTION_LINK_TARGET, reviewPoint)
              .choiceIndex(1)
              .inMemoryPlease();
      assertTrue(answerResult.correct);
    }
  }

  private QuizQuestion buildQuestion() {
    return makeMe.buildAQuestion(DESCRIPTION_LINK_TARGET, reviewPoint);
  }
}

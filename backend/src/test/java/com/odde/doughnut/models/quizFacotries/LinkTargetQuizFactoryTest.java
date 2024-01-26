package com.odde.doughnut.models.quizFacotries;

import static com.odde.doughnut.entities.QuizQuestionEntity.QuestionType.LINK_TARGET;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.in;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

import com.odde.doughnut.controllers.json.QuizQuestion;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.ReviewPoint;
import com.odde.doughnut.models.UserModel;
import com.odde.doughnut.testability.MakeMe;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class LinkTargetQuizFactoryTest {
  @Autowired MakeMe makeMe;
  UserModel userModel;
  Note top;
  Note target;
  Note source;
  Note anotherTarget;
  ReviewPoint reviewPoint;

  @BeforeEach
  void setup() {
    userModel = makeMe.aUser().toModelPlease();
    top = makeMe.aNote().creatorAndOwner(userModel).please();
    target = makeMe.aNote("target").under(top).please();
    source = makeMe.aNote("source").under(top).linkTo(target).please();
    anotherTarget = makeMe.aNote("another note").under(top).please();
    reviewPoint = makeMe.aReviewPointFor(source.getLinks().get(0)).inMemoryPlease();
    makeMe.refresh(top);
  }

  @Test
  @Disabled
  void shouldReturnNullIfCannotFindEnoughOptions() {
    makeMe.aLink().between(source, anotherTarget).please();
    makeMe.refresh(top);

    assertThat(buildLinkTargetQuizQuestion(), is(nullValue()));
  }

  @Nested
  class WhenThereAreMoreThanOneOptions {
    @BeforeEach
    void setup() {
      makeMe.refresh(top);
    }

    @Test
    void shouldIncludeRightAnswers() {
      QuizQuestion quizQuestion = buildLinkTargetQuizQuestion();
      assertThat(quizQuestion.getStem(), equalTo("<mark>source</mark> is a specialization of:"));
      assertThat(quizQuestion.getMainTopic(), equalTo(""));
      List<String> options = toOptionStrings(quizQuestion);
      assertThat(anotherTarget.getTopicConstructor(), in(options));
      assertThat(target.getTopicConstructor(), in(options));
    }
  }

  private QuizQuestion buildLinkTargetQuizQuestion() {
    return makeMe.buildAQuestion(LINK_TARGET, reviewPoint);
  }

  private List<String> toOptionStrings(QuizQuestion quizQuestion) {
    return quizQuestion.getChoices().stream().map(QuizQuestion.Choice::getDisplay).toList();
  }
}

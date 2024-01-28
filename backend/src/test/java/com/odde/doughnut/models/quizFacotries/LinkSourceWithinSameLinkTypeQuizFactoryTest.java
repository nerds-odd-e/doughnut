package com.odde.doughnut.models.quizFacotries;

import static com.odde.doughnut.entities.QuizQuestionEntity.QuestionType.LINK_SOURCE_WITHIN_SAME_LINK_TYPE;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import com.odde.doughnut.controllers.json.QuizQuestion;
import com.odde.doughnut.entities.Link;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.ReviewPoint;
import com.odde.doughnut.models.UserModel;
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
class LinkSourceWithinSameLinkTypeQuizFactoryTest {
  @Autowired MakeMe makeMe;
  UserModel userModel;
  Note top;
  Note target;
  Note source;
  Note anotherSource;
  Link sourceTarget;
  ReviewPoint reviewPoint;

  @BeforeEach
  void setup() {
    userModel = makeMe.aUser().toModelPlease();
    top = makeMe.aNote().creatorAndOwner(userModel).please();
    target = makeMe.aNote("sauce").under(top).please();
    source = makeMe.aNote("tomato sauce").under(top).please();
    sourceTarget = makeMe.aLink().between(source, target).please();
    Note cheese = makeMe.aNote("Note cheese").under(top).please();
    anotherSource = makeMe.aNote("blue cheese").under(top).linkTo(cheese).please();
    reviewPoint = makeMe.aReviewPointFor(sourceTarget).inMemoryPlease();
    makeMe.refresh(top);
    makeMe.refresh(anotherSource);
  }

  @Test
  void shouldReturnNullIfCannotFindEnoughOptions() {
    makeMe.aLink().between(anotherSource, target).please();
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
      assertThat(
          quizQuestion.getStem(),
          equalTo("Which one <em>is immediately a specialization of</em>:"));
      assertThat(quizQuestion.getMainTopic(), equalTo(target.getTopicConstructor()));
      List<String> options = toOptionStrings(quizQuestion);
      assertThat(anotherSource.getTopicConstructor(), in(options));
      assertThat(
          "tomato <mark title='Hidden text that is matching the answer'>[...]</mark>", in(options));
    }

    @Test
    void shouldIncludeOneLinkFromEachFillingOptions() {
      makeMe.aLink().between(anotherSource, top).please();
      QuizQuestion quizQuestion = buildLinkTargetQuizQuestion();
      List<String> options = toOptionStrings(quizQuestion);
      assertThat(options, hasSize(2));
    }
  }

  private QuizQuestion buildLinkTargetQuizQuestion() {
    return makeMe.buildAQuestion(LINK_SOURCE_WITHIN_SAME_LINK_TYPE, reviewPoint);
  }

  private List<String> toOptionStrings(QuizQuestion quizQuestion) {
    return quizQuestion.getChoices().stream().map(QuizQuestion.Choice::getDisplay).toList();
  }
}

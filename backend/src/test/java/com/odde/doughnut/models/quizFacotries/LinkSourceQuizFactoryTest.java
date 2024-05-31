package com.odde.doughnut.models.quizFacotries;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import com.odde.doughnut.controllers.dto.QuizQuestion;
import com.odde.doughnut.entities.LinkingNote;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.ReviewPoint;
import com.odde.doughnut.factoryServices.quizFacotries.factories.LinkSourceQuizFactory;
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
class LinkSourceQuizFactoryTest {
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
    target = makeMe.aNote("sauce").under(top).please();
    source = makeMe.aNote("tomato sauce").under(top).linkTo(target).please();
    anotherSource = makeMe.aNote("blue cheese").under(top).please();
    reviewPoint = makeMe.aReviewPointFor(source.getLinks().get(0)).inMemoryPlease();
    makeMe.refresh(top);
  }

  @Test
  void shouldReturnNullIfCannotFindEnoughOptions() {
    makeMe.aLink().between(anotherSource, target).please();
    makeMe.refresh(top);
    QuizQuestion actual = buildLinkSourceQuizQuestion();
    assertThat(actual, is(nullValue()));
  }

  @Nested
  class WhenThereAreMoreThanOneOptions {
    @Test
    void shouldIncludeRightAnswers() {
      QuizQuestion quizQuestion = buildLinkSourceQuizQuestion();
      assertThat(
          quizQuestion.getStem(),
          containsString("Which one <em>is immediately a specialization of</em>:"));
      assertThat(quizQuestion.getStem(), containsString(target.getTopicConstructor()));
      List<String> options = toOptionStrings(quizQuestion);
      assertThat(options, hasSize(2));
      assertThat(anotherSource.getTopicConstructor(), in(options));
      assertThat("tomato sauce", in(options));
    }
  }

  private QuizQuestion buildLinkSourceQuizQuestion() {
    return makeMe.buildAQuestion(
        new LinkSourceQuizFactory((LinkingNote) reviewPoint.getNote()), reviewPoint);
  }

  private List<String> toOptionStrings(QuizQuestion quizQuestion) {
    return quizQuestion.getChoices().stream().map(QuizQuestion.Choice::getDisplay).toList();
  }
}

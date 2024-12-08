package com.odde.doughnut.models.quizFacotries;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import com.odde.doughnut.entities.*;
import com.odde.doughnut.services.LinkQuestionType;
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
class ReificationSubjectQuizFactoryTest {
  @Autowired MakeMe makeMe;
  User user;
  Note top;
  Note object;
  Note subject;
  Note anotherSubject;
  Note reification;

  @BeforeEach
  void setup() {
    user = makeMe.aUser().please();
    top = makeMe.aNote().creatorAndOwner(user).please();
    object = makeMe.aNote("sauce").under(top).please();
    subject = makeMe.aNote("tomato sauce").under(top).linkTo(object).please();
    anotherSubject = makeMe.aNote("blue cheese").under(top).please();
    reification = subject.getLinks().get(0);
  }

  @Test
  void shouldReturnNullIfCannotFindEnoughOptions() {
    makeMe.aReification().between(anotherSubject, object).please();
    PredefinedQuestion actual = buildLinkSourcePredefinedQuestion();
    assertThat(actual, is(nullValue()));
  }

  @Nested
  class WhenThereAreMoreThanOneOptions {
    @Test
    void shouldIncludeRightAnswers() {
      PredefinedQuestion predefinedQuestion = buildLinkSourcePredefinedQuestion();
      assertThat(
          predefinedQuestion.getBareQuestion().getMultipleChoicesQuestion().getStem(),
          containsString("Which one <em>is immediately a specialization of</em>:"));
      assertThat(
          predefinedQuestion.getBareQuestion().getMultipleChoicesQuestion().getStem(),
          containsString(object.getTopicConstructor()));
      List<String> options =
          predefinedQuestion.getBareQuestion().getMultipleChoicesQuestion().getChoices();
      assertThat(options, hasSize(2));
      assertThat(anotherSubject.getTopicConstructor(), in(options));
      assertThat("tomato sauce", in(options));
    }
  }

  private PredefinedQuestion buildLinkSourcePredefinedQuestion() {
    return makeMe.buildAQuestionForLinkingNote(LinkQuestionType.LINK_SOURCE, reification, user);
  }
}

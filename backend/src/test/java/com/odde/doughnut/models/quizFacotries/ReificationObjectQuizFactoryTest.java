package com.odde.doughnut.models.quizFacotries;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.in;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

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
class ReificationObjectQuizFactoryTest {
  @Autowired MakeMe makeMe;
  User user;
  Note top;
  Note object;
  Note subject;
  Note anotherObject;
  Note reification;

  @BeforeEach
  void setup() {
    user = makeMe.aUser().please();
    top = makeMe.aNote().creatorAndOwner(user).please();
    object = makeMe.aNote("target").under(top).please();
    subject = makeMe.aNote("source").under(top).linkTo(object).please();
    anotherObject = makeMe.aNote("another note").under(top).please();
    reification = subject.getLinks().get(0);
  }

  @Test
  void shouldReturnNullIfCannotFindEnoughOptions() {
    makeMe.aReification().between(subject, anotherObject).please();

    assertThat(buildLinkTargetQuizQuestion(), is(nullValue()));
  }

  @Nested
  class WhenThereAreMoreThanOneOptions {
    @Test
    void shouldIncludeRightAnswers() {
      PredefinedQuestion predefinedQuestion = buildLinkTargetQuizQuestion();
      assertThat(
          predefinedQuestion.getBareQuestion().getMultipleChoicesQuestion().getStem(),
          equalTo("<mark>source</mark> is a specialization of:"));
      List<String> options =
          predefinedQuestion.getBareQuestion().getMultipleChoicesQuestion().getChoices();
      assertThat(anotherObject.getTopicConstructor(), in(options));
      assertThat(object.getTopicConstructor(), in(options));
    }
  }

  private PredefinedQuestion buildLinkTargetQuizQuestion() {
    return makeMe.buildAQuestionForLinkingNote(LinkQuestionType.LINK_TARGET, reification, user);
  }
}

package com.odde.doughnut.models.quizFacotries;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import com.odde.doughnut.entities.*;
import com.odde.doughnut.services.LinkQuestionType;
import com.odde.doughnut.testability.MakeMe;
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
class ClozeLinkTargetQuizFactoryTest {
  @Autowired MakeMe makeMe;
  User user;
  Note top;
  Note target;
  Note source;
  Note anotherSource;
  Note subjectNote;

  @BeforeEach
  void setup() {
    user = makeMe.aUser().please();
    top = makeMe.aNote().creatorAndOwner(user).please();
    target = makeMe.aNote("rome").under(top).please();
    source = makeMe.aNote("Rome is not built in a day").under(top).linkTo(target).please();
    subjectNote = source.getLinks().get(0);
    anotherSource = makeMe.aNote("pompeii").under(top).please();
  }

  @Nested
  class WhenThereAreMoreThanOneOptions {
    @Test
    void shouldIncludeRightAnswers() {
      PredefinedQuestion predefinedQuestion = (buildQuestion());
      assertThat(
          predefinedQuestion.getBareQuestion().getMultipleChoicesQuestion().getStem(),
          equalTo(
              "<mark><mark title='Hidden text that is matching the answer'>[...]</mark> is not built in a day</mark> is a specialization of:"));
    }
  }

  private PredefinedQuestion buildQuestion() {
    return makeMe.buildAQuestionForLinkingNote(
        LinkQuestionType.CLOZE_LINK_TARGET, subjectNote, user);
  }
}

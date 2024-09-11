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
class LinkSourceQuizFactoryTest {
  @Autowired MakeMe makeMe;
  User user;
  Note top;
  Note target;
  Note source;
  Note anotherSource;
  LinkingNote subjectNote;

  @BeforeEach
  void setup() {
    user = makeMe.aUser().please();
    top = makeMe.aNote().creatorAndOwner(user).please();
    target = makeMe.aNote("sauce").under(top).please();
    source = makeMe.aNote("tomato sauce").under(top).linkTo(target).please();
    anotherSource = makeMe.aNote("blue cheese").under(top).please();
    subjectNote = source.getLinks().get(0);
  }

  @Test
  void shouldReturnNullIfCannotFindEnoughOptions() {
    makeMe.aLink().between(anotherSource, target).please();
    PredefinedQuestion actual = buildLinkSourceQuizQuestion();
    assertThat(actual, is(nullValue()));
  }

  @Nested
  class WhenThereAreMoreThanOneOptions {
    @Test
    void shouldIncludeRightAnswers() {
      PredefinedQuestion predefinedQuestion = buildLinkSourceQuizQuestion();
      assertThat(
          predefinedQuestion.getMultipleChoicesQuestion().getStem(),
          containsString("Which one <em>is immediately a specialization of</em>:"));
      assertThat(
          predefinedQuestion.getMultipleChoicesQuestion().getStem(),
          containsString(target.getTopicConstructor()));
      List<String> options = predefinedQuestion.getMultipleChoicesQuestion().getChoices();
      assertThat(options, hasSize(2));
      assertThat(anotherSource.getTopicConstructor(), in(options));
      assertThat("tomato sauce", in(options));
    }
  }

  private PredefinedQuestion buildLinkSourceQuizQuestion() {
    return makeMe.buildAQuestionForLinkingNote(LinkQuestionType.LINK_SOURCE, subjectNote, user);
  }
}

package com.odde.doughnut.models;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.odde.doughnut.entities.*;
import com.odde.doughnut.factoryServices.quizFacotries.QuizQuestionFactory;
import com.odde.doughnut.factoryServices.quizFacotries.factories.*;
import com.odde.doughnut.testability.MakeMe;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class QuizQuestionGeneratorTest {
  @Autowired MakeMe makeMe;

  Note note;

  @BeforeEach
  void setup() {
    note = makeMe.aNote().please();
  }

  @Test
  void note() {
    makeMe.theNote(note).rememberSpelling().please();
    var questionTypes = getQuestionTypes(note);
    assertThat(
        questionTypes,
        contains(
            SpellingQuizFactory.class,
            ClozeTitleSelectionQuizFactory.class,
            PictureTitleSelectionQuizFactory.class,
            PictureSelectionQuizFactory.class));
  }

  @Test
  void linkExclusive() {
    Note note2 = makeMe.aNote().linkTo(note).please();
    var questionTypes = getQuestionTypes(note2.getLinks().get(0));
    assertThat(
        questionTypes,
        containsInAnyOrder(
            LinkTargetQuizFactory.class,
            LinkSourceQuizFactory.class,
            WhichSpecHasInstanceQuizFactory.class,
            FromSamePartAsQuizFactory.class,
            FromDifferentPartAsQuizFactory.class,
            DescriptionLinkTargetQuizFactory.class));
  }

  @Test
  void notAllLinkQuestionAreAvailableToAllLinkTypes() {
    Note note2 = makeMe.aNote().linkTo(note, LinkType.RELATED_TO).please();
    var questionTypes = getQuestionTypes(note2.getLinks().get(0));
    assertTrue(questionTypes.isEmpty());
  }

  private List<? extends Class<? extends QuizQuestionFactory>> getQuestionTypes(Note note) {
    return note.getQuizQuestionFactories(false).stream()
        .map(QuizQuestionFactory::getClass)
        .toList();
  }
}

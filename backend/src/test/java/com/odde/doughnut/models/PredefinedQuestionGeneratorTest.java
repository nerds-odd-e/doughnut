package com.odde.doughnut.models;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.odde.doughnut.entities.*;
import com.odde.doughnut.factoryServices.quizFacotries.PredefinedQuestionFactory;
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
class PredefinedQuestionGeneratorTest {
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
            SpellingPredefinedFactory.class,
            ClozeTitleSelectionPredefinedFactory.class,
            ImageTitleSelectionPredefinedFactory.class));
  }

  @Test
  void linkExclusive() {
    Note note2 = makeMe.aNote().linkTo(note).please();
    var questionTypes = getQuestionTypes(note2.getLinks().get(0));
    assertThat(
        questionTypes,
        containsInAnyOrder(
            LinkTargetPredefinedFactory.class,
            LinkSourcePredefinedFactory.class,
            WhichSpecHasInstancePredefinedFactory.class,
            FromSamePartAsPredefinedFactory.class,
            FromDifferentPartAsPredefinedFactory.class,
            DescriptionLinkTargetPredefinedFactory.class));
  }

  @Test
  void notAllLinkQuestionAreAvailableToAllLinkTypes() {
    Note note2 = makeMe.aNote().linkTo(note, LinkType.RELATED_TO).please();
    var questionTypes = getQuestionTypes(note2.getLinks().get(0));
    assertTrue(questionTypes.isEmpty());
  }

  private List<? extends Class<? extends PredefinedQuestionFactory>> getQuestionTypes(Note note) {
    return note.getQuizQuestionFactories(null).stream()
        .map(PredefinedQuestionFactory::getClass)
        .toList();
  }
}

package com.odde.doughnut.models;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.odde.doughnut.entities.*;
import com.odde.doughnut.factoryServices.quizFacotries.factories.*;
import com.odde.doughnut.services.LinkQuestionType;
import com.odde.doughnut.testability.MakeMe;
import java.util.Arrays;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class LinkingNoteQuestionTypesTest {
  @Autowired MakeMe makeMe;

  Note note;

  @BeforeEach
  void setup() {
    note = makeMe.aNote().please();
  }

  @Test
  void linkExclusive() {
    Note note2 = makeMe.aNote().linkTo(note).please();
    var questionTypes = note2.getLinks().get(0).getLinkType().getQuestionTypes();
    assertThat(
        Arrays.asList(questionTypes),
        containsInAnyOrder(
            LinkQuestionType.LINK_TARGET,
            LinkQuestionType.LINK_SOURCE,
            LinkQuestionType.WHICH_SPEC_HAS_INSTANCE,
            LinkQuestionType.FROM_SAME_PART_AS,
            LinkQuestionType.FROM_DIFFERENT_PART_AS,
            LinkQuestionType.DESCRIPTION_LINK_TARGET));
  }

  @Test
  void notAllLinkQuestionAreAvailableToAllLinkTypes() {
    Note note2 = makeMe.aNote().linkTo(note, LinkType.RELATED_TO).please();
    var questionTypes = note2.getLinks().get(0).getLinkType().getQuestionTypes();
    assertTrue(Arrays.asList(questionTypes).isEmpty());
  }
}

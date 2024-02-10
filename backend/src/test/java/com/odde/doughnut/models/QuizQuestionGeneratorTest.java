package com.odde.doughnut.models;

import static com.odde.doughnut.services.QuestionType.CLOZE_SELECTION;
import static com.odde.doughnut.services.QuestionType.DESCRIPTION_LINK_TARGET;
import static com.odde.doughnut.services.QuestionType.FROM_DIFFERENT_PART_AS;
import static com.odde.doughnut.services.QuestionType.FROM_SAME_PART_AS;
import static com.odde.doughnut.services.QuestionType.LINK_SOURCE;
import static com.odde.doughnut.services.QuestionType.LINK_TARGET;
import static com.odde.doughnut.services.QuestionType.PICTURE_SELECTION;
import static com.odde.doughnut.services.QuestionType.PICTURE_TITLE;
import static com.odde.doughnut.services.QuestionType.SPELLING;
import static com.odde.doughnut.services.QuestionType.WHICH_SPEC_HAS_INSTANCE;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.odde.doughnut.entities.*;
import com.odde.doughnut.services.QuestionType;
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
    List<QuestionType> questionTypes = getQuestionTypes(note);
    assertThat(
        questionTypes, contains(SPELLING, CLOZE_SELECTION, PICTURE_TITLE, PICTURE_SELECTION));
  }

  @Test
  void linkExclusive() {
    Note note2 = makeMe.aNote().linkTo(note).please();
    List<QuestionType> questionTypes = getQuestionTypes(note2.getLinks().get(0));
    assertThat(
        questionTypes,
        containsInAnyOrder(
            LINK_TARGET,
            LINK_SOURCE,
            WHICH_SPEC_HAS_INSTANCE,
            FROM_SAME_PART_AS,
            FROM_DIFFERENT_PART_AS,
            DESCRIPTION_LINK_TARGET));
  }

  @Test
  void notAllLinkQuestionAreAvailableToAllLinkTypes() {
    Note note2 = makeMe.aNote().linkTo(note, LinkType.RELATED_TO).please();
    List<QuestionType> questionTypes = getQuestionTypes(note2.getLinks().get(0));
    assertTrue(questionTypes.isEmpty());
  }

  private List<QuestionType> getQuestionTypes(Note note) {
    return note.getAvailableQuestionTypes(false);
  }
}

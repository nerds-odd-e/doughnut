package com.odde.doughnut.models;

import static com.odde.doughnut.entities.QuizQuestion.QuestionType.CLOZE_SELECTION;
import static com.odde.doughnut.entities.QuizQuestion.QuestionType.DESCRIPTION_LINK_TARGET;
import static com.odde.doughnut.entities.QuizQuestion.QuestionType.FROM_DIFFERENT_PART_AS;
import static com.odde.doughnut.entities.QuizQuestion.QuestionType.FROM_SAME_PART_AS;
import static com.odde.doughnut.entities.QuizQuestion.QuestionType.LINK_SOURCE;
import static com.odde.doughnut.entities.QuizQuestion.QuestionType.LINK_TARGET;
import static com.odde.doughnut.entities.QuizQuestion.QuestionType.PICTURE_SELECTION;
import static com.odde.doughnut.entities.QuizQuestion.QuestionType.PICTURE_TITLE;
import static com.odde.doughnut.entities.QuizQuestion.QuestionType.SPELLING;
import static com.odde.doughnut.entities.QuizQuestion.QuestionType.WHICH_SPEC_HAS_INSTANCE;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.odde.doughnut.entities.Link;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.QuizQuestion;
import com.odde.doughnut.entities.ReviewPoint;
import com.odde.doughnut.testability.MakeMe;
import java.util.List;
import org.junit.jupiter.api.Test;

class QuizQuestionGeneratorTest {
  MakeMe makeMe = MakeMe.makeMeWithoutFactoryService();
  Note note = makeMe.aNote().inMemoryPlease();

  @Test
  void note() {
    makeMe.theNote(note).rememberSpelling();
    ReviewPoint reviewPoint = makeMe.aReviewPointFor(note).inMemoryPlease();
    List<QuizQuestion.QuestionType> questionTypes = getQuestionTypes(reviewPoint);
    assertThat(
        questionTypes, contains(SPELLING, CLOZE_SELECTION, PICTURE_TITLE, PICTURE_SELECTION));
  }

  @Test
  void linkExclusive() {
    Note note2 = makeMe.aNote().linkTo(note).inMemoryPlease();
    ReviewPoint reviewPoint = makeMe.aReviewPointFor(note2.getLinks().get(0)).inMemoryPlease();
    List<QuizQuestion.QuestionType> questionTypes = getQuestionTypes(reviewPoint);
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
    Note note2 = makeMe.aNote().linkTo(note, Link.LinkType.RELATED_TO).inMemoryPlease();
    ReviewPoint reviewPoint = makeMe.aReviewPointFor(note2.getLinks().get(0)).inMemoryPlease();
    List<QuizQuestion.QuestionType> questionTypes = getQuestionTypes(reviewPoint);
    assertTrue(questionTypes.isEmpty());
  }

  private List<QuizQuestion.QuestionType> getQuestionTypes(ReviewPoint reviewPoint) {
    return reviewPoint.availableQuestionTypes();
  }
}

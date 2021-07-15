package com.odde.doughnut.models;

import com.odde.doughnut.entities.Link;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.QuizQuestion;
import com.odde.doughnut.entities.ReviewPoint;
import com.odde.doughnut.models.randomizers.NonRandomizer;
import com.odde.doughnut.testability.MakeMe;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.odde.doughnut.entities.QuizQuestion.QuestionType.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QuizQuestionGeneratorTest {
    MakeMe makeMe = new MakeMe();
    private Randomizer randomizer = new NonRandomizer();
    Note note = makeMe.aNote().inMemoryPlease();

    @Test
    void note() {
        makeMe.theNote(note).rememberSpelling();
        ReviewPoint reviewPoint = makeMe.aReviewPointFor(note).inMemoryPlease();
        List<QuizQuestion.QuestionType> questionTypes = getQuestionTypes(reviewPoint);
        assertThat(questionTypes, contains(SPELLING, CLOZE_SELECTION, PICTURE_TITLE));
    }

    @Test
    void linkExclusive() {
        Note note2 = makeMe.aNote().linkTo(note).inMemoryPlease();
        ReviewPoint reviewPoint = makeMe.aReviewPointFor(note2.getLinks().get(0)).inMemoryPlease();
        List<QuizQuestion.QuestionType> questionTypes = getQuestionTypes(reviewPoint);
        assertThat(questionTypes, containsInAnyOrder(LINK_TARGET, LINK_SOURCE_EXCLUSIVE));
    }

    @Test
    void notAllLinkQuestionAreAvailableToAllLinkTypes() {
        Note note2 = makeMe.aNote().linkTo(note, Link.LinkType.RELATED_TO).inMemoryPlease();
        ReviewPoint reviewPoint = makeMe.aReviewPointFor(note2.getLinks().get(0)).inMemoryPlease();
        List<QuizQuestion.QuestionType> questionTypes = getQuestionTypes(reviewPoint);
        assertTrue(questionTypes.isEmpty());
    }

    private List<QuizQuestion.QuestionType> getQuestionTypes(ReviewPoint reviewPoint) {
        QuizQuestionGenerator generator = new QuizQuestionGenerator(reviewPoint, randomizer);
        return generator.availableQuestionTypes();
    }

}
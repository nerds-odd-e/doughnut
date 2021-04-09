package com.odde.doughnut.models;

import com.odde.doughnut.entities.LinkEntity;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.ReviewPointEntity;
import com.odde.doughnut.models.randomizers.NonRandomizer;
import com.odde.doughnut.testability.MakeMe;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.odde.doughnut.models.QuizQuestion.QuestionType.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QuizQuestionGeneratorTest {
    MakeMe makeMe = new MakeMe();
    private Randomizer randomizer = new NonRandomizer();
    Note note = makeMe.aNote().inMemoryPlease();

    @Test
    void clozeSelection() {
        ReviewPointEntity reviewPoint = makeMe.aReviewPointFor(note).inMemoryPlease();
        List<QuizQuestion.QuestionType> questionTypes = getQuestionTypes(reviewPoint);
        assertThat(questionTypes, contains(CLOZE_SELECTION));
    }

    @Test
    void spelling() {
        makeMe.theNote(note).rememberSpelling();
        ReviewPointEntity reviewPoint = makeMe.aReviewPointFor(note).inMemoryPlease();
        List<QuizQuestion.QuestionType> questionTypes = getQuestionTypes(reviewPoint);
        assertThat(questionTypes, contains(SPELLING, CLOZE_SELECTION));
    }

    @Test
    void linkExclusive() {
        Note note2 = makeMe.aNote().linkTo(note).inMemoryPlease();
        ReviewPointEntity reviewPoint = makeMe.aReviewPointFor(note2.getLinks().get(0)).inMemoryPlease();
        List<QuizQuestion.QuestionType> questionTypes = getQuestionTypes(reviewPoint);
        assertThat(questionTypes, containsInAnyOrder(LINK_TARGET, LINK_SOURCE_EXCLUSIVE));
    }

    @Test
    void notAllLinkQuestionAreAvailableToAllLinkTypes() {
        Note note2 = makeMe.aNote().linkTo(note, LinkEntity.LinkType.RELATED_TO).inMemoryPlease();
        ReviewPointEntity reviewPoint = makeMe.aReviewPointFor(note2.getLinks().get(0)).inMemoryPlease();
        List<QuizQuestion.QuestionType> questionTypes = getQuestionTypes(reviewPoint);
        assertTrue(questionTypes.isEmpty());
    }

    private List<QuizQuestion.QuestionType> getQuestionTypes(ReviewPointEntity reviewPoint) {
        QuizQuestionGenerator generator = new QuizQuestionGenerator(reviewPoint, randomizer);
        return generator.availableQuestionTypes();
    }

}
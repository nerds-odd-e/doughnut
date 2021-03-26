package com.odde.doughnut.models;

import com.odde.doughnut.entities.NoteEntity;
import com.odde.doughnut.entities.ReviewPointEntity;
import com.odde.doughnut.models.randomizers.NonRandomizer;
import com.odde.doughnut.testability.MakeMe;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.odde.doughnut.models.QuizQuestion.QuestionType.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;

class QuizQuestionGeneratorTest {
    MakeMe makeMe = new MakeMe();
    private Randomizer randomizer = new NonRandomizer();

    @Test
    void clozeSelection() {
        NoteEntity note = makeMe.aNote().inMemoryPlease();
        ReviewPointEntity reviewPoint = makeMe.aReviewPointFor(note).inMemoryPlease();
        QuizQuestionGenerator generator = new QuizQuestionGenerator(reviewPoint, randomizer, null);
        List<QuizQuestion.QuestionType> questionTypes = generator.availableQuestionTypes();
        assertThat(questionTypes, contains(CLOZE_SELECTION));
    }

    @Test
    void linkExclusive() {
        NoteEntity note1 = makeMe.aNote().inMemoryPlease();
        NoteEntity note2 = makeMe.aNote().linkTo(note1).inMemoryPlease();
        ReviewPointEntity reviewPoint = makeMe.aReviewPointFor(note2.getLinks().get(0)).inMemoryPlease();
        QuizQuestionGenerator generator = new QuizQuestionGenerator(reviewPoint, randomizer, null);
        List<QuizQuestion.QuestionType> questionTypes = generator.availableQuestionTypes();
        assertThat(questionTypes, containsInAnyOrder(LINK_TARGET, LINK_SOURCE_EXCLUSIVE));
    }

}
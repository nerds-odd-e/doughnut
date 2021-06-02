package com.odde.doughnut.entities;

import com.odde.doughnut.testability.MakeMe;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static com.odde.doughnut.entities.QuizQuestion.QuestionType.CLOZE_SELECTION;
import static com.odde.doughnut.entities.QuizQuestion.QuestionType.LINK_SOURCE_EXCLUSIVE;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AnswerTest {
    MakeMe makeMe = new MakeMe();

    @Nested
    class ClozeSelectionQuestion {
        Note note;
        ReviewPoint reviewPoint;

        @BeforeEach
        void setup() {
            note = makeMe.aNote("this / that").inMemoryPlease();
            reviewPoint = makeMe.aReviewPointFor(note).inMemoryPlease();
        }

        @Test
        void correct() {
            Answer answer = makeMe.anAnswerFor(reviewPoint)
                    .type(CLOZE_SELECTION)
                    .answer("this")
                    .inMemoryPlease();
            assertFalse(answer.isCorrect());
        }
    }

    @Nested
    class LinkTargetExclusiveQuestion {
        Note target;
        Note source;
        ReviewPoint reviewPoint;

        @BeforeEach
        void setup() {
            Note top = makeMe.aNote().inMemoryPlease();
            target = makeMe.aNote("target").under(top).inMemoryPlease();
            source = makeMe.aNote("source").under(top).linkTo(target).inMemoryPlease();
            reviewPoint = makeMe.aReviewPointFor(source.getLinks().get(0)).inMemoryPlease();
        }

        @Test
        void correct () {
            Answer answer = makeMe.anAnswerFor(reviewPoint)
                    .type(LINK_SOURCE_EXCLUSIVE)
                    .answer("blah")
                    .inMemoryPlease();
            assertTrue(answer.isCorrect());
        }

        @Test
        void wrong () {
            Answer answer = makeMe.anAnswerFor(reviewPoint)
                    .type(LINK_SOURCE_EXCLUSIVE)
                    .answer(source.getTitle())
                    .inMemoryPlease();
            assertFalse(answer.isCorrect());
        }
    }

}
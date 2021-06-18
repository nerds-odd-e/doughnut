package com.odde.doughnut.entities;

import com.odde.doughnut.testability.MakeMe;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static com.odde.doughnut.entities.QuizQuestion.QuestionType.CLOZE_SELECTION;
import static com.odde.doughnut.entities.QuizQuestion.QuestionType.LINK_SOURCE_EXCLUSIVE;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AnswerResultTest {
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
            AnswerResult answer = makeMe.anAnswerFor(reviewPoint)
                    .type(CLOZE_SELECTION)
                    .answer("this")
                    .inMemoryPlease();
            assertTrue(answer.isCorrect());
        }

        @Test
        void literalAnswer() {
            AnswerResult answerResult = makeMe.anAnswerFor(reviewPoint)
                    .type(CLOZE_SELECTION)
                    .answer("this / that")
                    .inMemoryPlease();
            assertTrue(answerResult.isCorrect());
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
            AnswerResult answerResult = makeMe.anAnswerFor(reviewPoint)
                    .type(LINK_SOURCE_EXCLUSIVE)
                    .answer("blah")
                    .inMemoryPlease();
            assertTrue(answerResult.isCorrect());
        }

        @Test
        void wrong () {
            AnswerResult answerResult = makeMe.anAnswerFor(reviewPoint)
                    .type(LINK_SOURCE_EXCLUSIVE)
                    .answer(source.getTitle())
                    .inMemoryPlease();
            assertFalse(answerResult.isCorrect());
        }
    }

}
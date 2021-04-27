package com.odde.doughnut.entities;

import com.odde.doughnut.testability.MakeMe;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static com.odde.doughnut.entities.QuizQuestion.QuestionType.LINK_SOURCE_EXCLUSIVE;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AnswerTest {
    MakeMe makeMe = new MakeMe();
    Note target;
    Note source;

    @BeforeEach
    void setup() {
        Note top = makeMe.aNote().inMemoryPlease();
        target = makeMe.aNote("target").under(top).inMemoryPlease();
        source = makeMe.aNote("source").under(top).linkTo(target).inMemoryPlease();
    }

    @Nested
    class LinkTargetExclusiveQuestion {
        ReviewPoint reviewPoint;
        @BeforeEach
        void setup() {
            reviewPoint = makeMe.aReviewPointFor(source.getLinks().get(0)).inMemoryPlease();
        }

        @Test
        void correct () {
            Answer answer = makeMe.anAnswerFor(reviewPoint)
                    .type(LINK_SOURCE_EXCLUSIVE)
                    .answer("blah")
                    .inMemoryPlease();
            assertTrue(answer.checkAnswer());
        }

        @Test
        void wrong () {
            Answer answer = makeMe.anAnswerFor(reviewPoint)
                    .type(LINK_SOURCE_EXCLUSIVE)
                    .answer(source.getTitle())
                    .inMemoryPlease();
            assertFalse(answer.checkAnswer());
        }

    }

}
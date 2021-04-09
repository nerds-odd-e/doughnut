package com.odde.doughnut.entities;

import com.odde.doughnut.testability.MakeMe;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static com.odde.doughnut.models.QuizQuestion.QuestionType.LINK_SOURCE_EXCLUSIVE;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AnswerTest {
    MakeMe makeMe = new MakeMe();
    Note target;
    Note source;

    @BeforeEach
    void setup() {
        target = makeMe.aNote("target").inMemoryPlease();
        source = makeMe.aNote("source").linkTo(target).inMemoryPlease();
    }

    @Nested
    class LinkTargetExclusiveQuestion {
        ReviewPointEntity reviewPointEntity;
        @BeforeEach
        void setup() {
            reviewPointEntity = makeMe.aReviewPointFor(source.getLinks().get(0)).inMemoryPlease();
        }

        @Test
        void correct () {
            Answer answer = makeMe.anAnswerFor(reviewPointEntity)
                    .type(LINK_SOURCE_EXCLUSIVE)
                    .answer("blah")
                    .inMemoryPlease();
            assertTrue(answer.checkAnswer());
        }

        @Test
        void wrong () {
            Answer answer = makeMe.anAnswerFor(reviewPointEntity)
                    .type(LINK_SOURCE_EXCLUSIVE)
                    .answer(source.getTitle())
                    .inMemoryPlease();
            assertFalse(answer.checkAnswer());
        }

    }

}
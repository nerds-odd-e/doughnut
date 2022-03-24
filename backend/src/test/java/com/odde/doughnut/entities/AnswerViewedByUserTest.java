package com.odde.doughnut.entities;

import com.odde.doughnut.testability.MakeMe;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;

import static com.odde.doughnut.entities.QuizQuestion.QuestionType.CLOZE_SELECTION;
import static com.odde.doughnut.entities.QuizQuestion.QuestionType.LINK_SOURCE_EXCLUSIVE;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = {"classpath:repository.xml"})
@Transactional
class AnswerViewedByUserTest {
    @Autowired
    MakeMe makeMe;

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
            AnswerViewedByUser answer = makeMe.anAnswerFor(reviewPoint)
                    .type(CLOZE_SELECTION)
                    .answer("this")
                    .inMemoryPlease();
            assertTrue(answer.correct);
        }

        @Test
        void literalAnswer() {
            AnswerViewedByUser answerResult = makeMe.anAnswerFor(reviewPoint)
                    .type(CLOZE_SELECTION)
                    .answer("this / that")
                    .inMemoryPlease();
            assertTrue(answerResult.correct);
        }

    }

    @Nested
    class LinkSourceExclusiveQuestion {
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
            AnswerViewedByUser answerResult = makeMe.anAnswerFor(reviewPoint)
                    .type(LINK_SOURCE_EXCLUSIVE)
                    .answer("blah")
                    .inMemoryPlease();
            assertTrue(answerResult.correct);
        }

        @Test
        void wrong () {
            AnswerViewedByUser answerResult = makeMe.anAnswerFor(reviewPoint)
                    .type(LINK_SOURCE_EXCLUSIVE)
                    .answer(source.getTitle())
                    .inMemoryPlease();
            assertFalse(answerResult.correct);
        }
    }

}
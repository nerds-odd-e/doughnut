package com.odde.doughnut.controllers;

import com.odde.doughnut.entities.*;
import com.odde.doughnut.factoryServices.ModelFactoryService;
import com.odde.doughnut.models.UserModel;
import com.odde.doughnut.testability.MakeMe;
import com.odde.doughnut.testability.TestabilitySettings;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import javax.validation.Valid;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = {"classpath:repository.xml"})
@Transactional
class RestReviewsControllerTests {
    @Autowired
    ModelFactoryService modelFactoryService;

    @Autowired
    MakeMe makeMe;
    private UserModel userModel;
    private TestabilitySettings testabilitySettings = new TestabilitySettings();

    @BeforeEach
    void setup() {
        userModel = makeMe.aUser().toModelPlease();
    }

    RestReviewsController controller() {
        return new RestReviewsController(modelFactoryService, new TestCurrentUserFetcher(userModel), testabilitySettings);
    }

    @Nested
    class overall {
        @Test
        void shouldNotBeAbleToSeeNoteIDontHaveAccessTo() {
            userModel = makeMe.aNullUserModel();
            assertThrows(ResponseStatusException.class, () -> controller().overview());
        }
    }

    @Nested
    class repeat {
        @Test
        void shouldNotBeAbleToSeeNoteIDontHaveAccessTo() {
            userModel = makeMe.aNullUserModel();
            assertThrows(ResponseStatusException.class, () -> controller().repeatReview());
        }
    }

    @Nested
    class answer {
        ReviewPoint reviewPoint;
        Note note1;
        @BeforeEach
        void setup() {
            note1 = makeMe.aNote().please();
//            Note note2 = makeMe.aNote().please();
//            Link link = makeMe.aLink().between(note1, note2).please();
            reviewPoint = makeMe.aReviewPointFor(note1).by(userModel).please();
        }

        @Test
        void shouldValidateTheAnswer() {
            Answer answer = new Answer();
            answer.setQuestionType(QuizQuestion.QuestionType.CLOZE_SELECTION);
            answer.setAnswerNoteId(note1.getId());
            AnswerResult answerResult = controller().answerQuiz(reviewPoint, answer);
            assertTrue(answerResult.isCorrect());
        }

        @Test
        void shouldNotBeAbleToSeeNoteIDontHaveAccessTo() {
            userModel = makeMe.aNullUserModel();
            Answer answer = new Answer();
            assertThrows(ResponseStatusException.class, () -> controller().answerQuiz(reviewPoint, answer));
        }

    }

    @Nested
    class evaluate {
        @Test
        void shouldNotBeAbleToSeeNoteIDontHaveAccessTo() {
            userModel = makeMe.aNullUserModel();
            ReviewPoint reviewPoint = new ReviewPoint();
            assertThrows(ResponseStatusException.class, () -> controller().selfEvaluate(reviewPoint, "happy"));
        }
    }
    @Nested
    class WhenThereIsAReviewPoint {
        ReviewPoint rp;
        final int expectedSatisfyingForgettingCurveIndex = 110;

        @BeforeEach
        void setup() {
            rp = makeMe.aReviewPointFor(makeMe.aNote().please()).by(userModel).please();
        }


        @Test
        void repeat() {
            controller().selfEvaluate(rp, "\"satisfying\"");
            assertThat(rp.getForgettingCurveIndex(), equalTo(expectedSatisfyingForgettingCurveIndex));
            assertThat(rp.getRepetitionCount(), equalTo(1));
        }

        @Test
        void repeatSad() {
            controller().selfEvaluate(rp, "\"sad\"");
            assertThat(rp.getForgettingCurveIndex(), lessThan(expectedSatisfyingForgettingCurveIndex));
            assertThat(rp.getRepetitionCount(), equalTo(1));
        }

        @Test
        void repeatHappy() {
            controller().selfEvaluate(rp, "\"happy\"");
            assertThat(rp.getForgettingCurveIndex(), greaterThan(expectedSatisfyingForgettingCurveIndex));
            assertThat(rp.getRepetitionCount(), equalTo(1));
        }

        @Test
        void repeatUnknown() {
            assertThrows(ResponseStatusException.class, ()-> controller().selfEvaluate(rp, "unknown"));
        }

    }

}

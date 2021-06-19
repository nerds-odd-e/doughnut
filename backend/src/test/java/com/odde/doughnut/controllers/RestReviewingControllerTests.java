package com.odde.doughnut.controllers;

import com.odde.doughnut.entities.Answer;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.ReviewPoint;
import com.odde.doughnut.entities.User;
import com.odde.doughnut.entities.json.NoteViewedByUser;
import com.odde.doughnut.exceptions.NoAccessRightException;
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
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = {"classpath:repository.xml"})
@Transactional
class RestReviewingControllerTests {
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
            userModel = makeMe.modelFactoryService.toUserModel(null);
            assertThrows(ResponseStatusException.class, () -> controller().overview());
        }
    }

    @Nested
    class repeat {
        @Test
        void shouldNotBeAbleToSeeNoteIDontHaveAccessTo() {
            userModel = makeMe.modelFactoryService.toUserModel(null);
            assertThrows(ResponseStatusException.class, () -> controller().repeatReview());
        }
    }

    @Nested
    class answer {
        @Test
        void shouldNotBeAbleToSeeNoteIDontHaveAccessTo() {
            userModel = makeMe.modelFactoryService.toUserModel(null);
            ReviewPoint reviewPoint = new ReviewPoint();
            @Valid Answer answer = new Answer();
            assertThrows(ResponseStatusException.class, () -> controller().answerQuiz(reviewPoint, answer));
        }
    }

    @Nested
    class evaluate {
        @Test
        void shouldNotBeAbleToSeeNoteIDontHaveAccessTo() {
            userModel = makeMe.modelFactoryService.toUserModel(null);
            ReviewPoint reviewPoint = new ReviewPoint();
            assertThrows(ResponseStatusException.class, () -> controller().selfEvaluate(reviewPoint, "happy"));
        }
    }

}

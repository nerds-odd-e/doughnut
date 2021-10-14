package com.odde.doughnut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.odde.doughnut.entities.Circle;
import com.odde.doughnut.entities.NoteContent;
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
import org.springframework.validation.BindException;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = {"classpath:repository.xml"})
@Transactional
class RestCircleControllerTest {
    @Autowired
    ModelFactoryService modelFactoryService;

    @Autowired
    MakeMe makeMe;
    private UserModel userModel;
    RestCircleController controller;
    private TestabilitySettings testabilitySettings = new TestabilitySettings();

    @BeforeEach
    void setup() {
        userModel = makeMe.aUser().toModelPlease();
        controller = new RestCircleController(modelFactoryService, new TestCurrentUserFetcher(userModel), testabilitySettings);
    }

    @Nested
    class circleIndex {
        @Test
        void itShouldNotAllowNonMemberToSeeACircle() {
            controller = new RestCircleController(modelFactoryService, new TestCurrentUserFetcher(makeMe.aNullUserModel()), testabilitySettings);
            assertThrows(ResponseStatusException.class, ()->{
                controller.index();
            });
        }
    }

    @Nested
    class showNoteTest {
        @Test
        void whenTheUserIsNotAMemberOfTheCircle() {
            Circle circle = makeMe.aCircle().please();
            NoteContent noteContent = makeMe.aNote().inMemoryPlease().getNoteContent();
            assertThrows(NoAccessRightException.class, () -> controller.createNotebook(circle, noteContent));
        }
    }

    @Nested
    class ShowCircle {

        @Test
        void itShouldAskToLoginOfVisitorIsNotLogin() {
            Circle circle = makeMe.aCircle().please();
            controller = new RestCircleController(modelFactoryService, new TestCurrentUserFetcher(makeMe.aNullUserModel()), testabilitySettings);
            assertThrows(ResponseStatusException.class, ()->{
                controller.showCircle(circle);
            });
        }

        @Test
        void itShouldNotAllowNonMemberToSeeACircle() {
            Circle circle = makeMe.aCircle().please();
            assertThrows(NoAccessRightException.class, ()->{
                controller.showCircle(circle);
            });
        }
    }

    @Nested
    class JoinCircle {
        @Test
        void validationFailed() {
            RestCircleController.CircleJoiningByInvitation entity = new RestCircleController.CircleJoiningByInvitation();
            entity.setInvitationCode("short");
            BindException exception = assertThrows(BindException.class, ()->controller.joinCircle(entity));
            assertThat(exception.getErrorCount(), equalTo(1));
        }

        @Test
        void userAlreadyInCircle() {
            Circle circle = makeMe.aCircle().hasMember(userModel).please();
            RestCircleController.CircleJoiningByInvitation entity = new RestCircleController.CircleJoiningByInvitation();
            entity.setInvitationCode(circle.getInvitationCode());
            BindException exception = assertThrows(BindException.class, ()->controller.joinCircle(entity));
            assertThat(exception.getErrorCount(), equalTo(1));
        }

    }
}
package com.odde.doughnut.controllers;

import com.odde.doughnut.entities.*;
import com.odde.doughnut.exceptions.NoAccessRightException;
import com.odde.doughnut.models.UserModel;
import com.odde.doughnut.services.ModelFactoryService;
import com.odde.doughnut.testability.MakeMe;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.validation.BindingResult;
import org.springframework.validation.ObjectError;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = {"classpath:repository.xml"})
@Transactional
class CircleControllerTests {
    @Autowired
    ModelFactoryService modelFactoryService;
    final ExtendedModelMap model = new ExtendedModelMap();

    @Autowired
    MakeMe makeMe;
    private UserModel userModel;
    CircleController controller;

    @BeforeEach
    void setup() {
        userModel = makeMe.aUser().toModelPlease();
        controller = new CircleController(new TestCurrentUserFetcher(userModel), modelFactoryService);
    }

    @Nested
    class ShowCircle {
        @Test
        void itShouldNotAllowNonMemberToSeeACircle() {
            CircleEntity circleEntity = makeMe.aCircle().please();
            assertThrows(NoAccessRightException.class, ()->{
                controller.showCircle(circleEntity, model);
            });
        }
    }

    @Nested
    class JoinCircle {
        @Test
        void validationFailed() {
            CircleJoiningByInvitationEntity entity = new CircleJoiningByInvitationEntity();
            entity.setInvitationCode("not exist");
            BindingResult bindingResult = makeMe.failedBindingResult();
            assertThat(controller.joinCircle(entity, bindingResult), equalTo("circles/join"));
        }

        @Test
        void circleDoesNotExist() {
            CircleJoiningByInvitationEntity entity = new CircleJoiningByInvitationEntity();
            entity.setInvitationCode("not exist");
            BindingResult bindingResult = mock(BindingResult.class);
            assertThat(controller.joinCircle(entity, bindingResult), equalTo("circles/join"));
            ArgumentCaptor<ObjectError> errorArgumentCaptor = ArgumentCaptor.forClass(ObjectError.class);
            verify(bindingResult).rejectValue(eq("invitationCode"), anyString(), anyString());
        }

        @Test
        void userAlreadyInCircle() {
            CircleEntity circleEntity = makeMe.aCircle().hasMember(userModel).please();
            CircleJoiningByInvitationEntity entity = new CircleJoiningByInvitationEntity();
            entity.setInvitationCode(circleEntity.getInvitationCode());
            BindingResult bindingResult = mock(BindingResult.class);
            assertThat(controller.joinCircle(entity, bindingResult), equalTo("circles/join"));
            ArgumentCaptor<ObjectError> errorArgumentCaptor = ArgumentCaptor.forClass(ObjectError.class);
            verify(bindingResult).rejectValue(eq("invitationCode"), anyString(), anyString());
        }

    }

}

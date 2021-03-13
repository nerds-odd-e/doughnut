package com.odde.doughnut.controllers;

import com.odde.doughnut.models.UserModel;
import com.odde.doughnut.testability.MakeMe;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = {"classpath:repository.xml"})
@Transactional
class UserControllerTest {
    @Autowired
    MakeMe makeMe;
    UserModel userModel;
    UserController controller;


    @BeforeEach
    void setup() {
        userModel = makeMe.aUser().toModelPlease();
        controller = new UserController(makeMe.modelFactoryService, new TestCurrentUserFetcher(userModel));
    }

    @Test
    void updateUserSuccessfully() {
        String response = controller.updateUser(userModel.getEntity(), makeMe.successfulBindingResult());
        assertThat(response, equalTo("redirect:/"));
    }

    @Test
    void updateUserValidationFailed() {
        String response = controller.updateUser(userModel.getEntity(), makeMe.failedBindingResult());
        assertThat(response, equalTo("users/edit"));
    }

}
package com.odde.doughnut.controllers;

import com.odde.doughnut.entities.User;
import com.odde.doughnut.exceptions.NoAccessRightException;
import com.odde.doughnut.models.UserModel;
import com.odde.doughnut.testability.MakeMe;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.ExtendedModelMap;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = {"classpath:repository.xml"})
@Transactional
class FailureReportControllerTest {
    @Autowired
    MakeMe makeMe;
    UserModel userModel;
    FailureReportController controller;
    final ExtendedModelMap model = new ExtendedModelMap();

    @BeforeEach
    void setup() {
        userModel = makeMe.aDeveloper().toModelPlease();
        controller = new FailureReportController(makeMe.modelFactoryService,
                        new TestCurrentUserFetcher(userModel));
    }

    @Test
    void failureReportListSuccessfully() throws NoAccessRightException {
        String response = controller.failureReport(model);
        assertThat(response, equalTo("failure-report-list/index"));
    }

//    @Test
//    void updateUserValidationFailed() throws NoAccessRightException {
//        String response = controller.updateUser(userModel.getEntity(), makeMe.failedBindingResult());
//        assertThat(response, equalTo("users/edit"));
//    }

}
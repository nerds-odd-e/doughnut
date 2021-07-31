package com.odde.doughnut.controllers;

import com.odde.doughnut.factoryServices.ModelFactoryService;
import com.odde.doughnut.models.UserModel;
import com.odde.doughnut.testability.MakeMe;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = {"classpath:repository.xml"})
@Transactional
class RestNotebookControllerTest {
    @Autowired
    ModelFactoryService modelFactoryService;

    @Autowired
    MakeMe makeMe;
    private UserModel userModel;
    RestNotebookController controller;

    @Nested
    class showNoteTest {
        @Test
        void whenNotLogin() {
            userModel = modelFactoryService.toUserModel(null);
            controller = new RestNotebookController(modelFactoryService, new TestCurrentUserFetcher(userModel));
            assertThrows(ResponseStatusException.class, () -> controller.myNotebooks());
        }
    }
}
package com.odde.doughnut.controllers;

import com.odde.doughnut.entities.Circle;
import com.odde.doughnut.entities.NoteContent;
import com.odde.doughnut.exceptions.NoAccessRightException;
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

import static org.junit.jupiter.api.Assertions.assertThrows;

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

    @Nested
    class showNoteTest {
        @Test
        void whenTheUserIsNotAMemberOfTheCircle() {
            userModel = makeMe.aUser().toModelPlease();
            controller = new RestCircleController(modelFactoryService, new TestCurrentUserFetcher(userModel));
            Circle circle = makeMe.aCircle().please();
            NoteContent noteContent = makeMe.aNote().inMemoryPlease().getNoteContent();
            assertThrows(NoAccessRightException.class, () -> controller.createNotebook(circle, noteContent));
        }
    }
}
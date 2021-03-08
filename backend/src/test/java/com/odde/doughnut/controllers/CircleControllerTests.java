package com.odde.doughnut.controllers;

import com.odde.doughnut.entities.CircleEntity;
import com.odde.doughnut.entities.NoteEntity;
import com.odde.doughnut.entities.NoteMotionEntity;
import com.odde.doughnut.entities.UserEntity;
import com.odde.doughnut.entities.repositories.LinkRepository;
import com.odde.doughnut.exceptions.NoAccessRightException;
import com.odde.doughnut.models.UserModel;
import com.odde.doughnut.services.ModelFactoryService;
import com.odde.doughnut.testability.MakeMe;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.validation.BindingResult;
import org.springframework.web.servlet.view.RedirectView;

import javax.persistence.EntityManager;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;

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
}

package com.odde.doughnut.controllers;

import com.odde.doughnut.entities.*;
import com.odde.doughnut.exceptions.NoAccessRightException;
import com.odde.doughnut.factoryServices.ModelFactoryService;
import com.odde.doughnut.models.UserModel;
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

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.Locale;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = {"classpath:repository.xml"})
@Transactional
class NoteControllerTests {
    @Autowired
    ModelFactoryService modelFactoryService;

    @Autowired
    MakeMe makeMe;
    private UserModel userModel;
    NoteController controller;

    @BeforeEach
    void setup() {
        userModel = makeMe.aUser().toModelPlease();
        controller = new NoteController(new TestCurrentUserFetcher(userModel), modelFactoryService);
    }



    @Nested
    class MoveNoteTest {
        User anotherUser;
        Note note1;
        Note note2;

        @BeforeEach
        void setup() {
            anotherUser = makeMe.aUser().please();
            note1 = makeMe.aNote().byUser(anotherUser).please();
            note2 = makeMe.aNote().byUser(userModel).please();
        }

        @Test
        void shouldNotAllowMoveOtherPeoplesNote() {
            NoteMotion motion = new NoteMotion(note2, false);
            assertThrows(NoAccessRightException.class, () ->
                    controller.moveNote(note1, motion)
            );
        }

        @Test
        void shouldNotAllowMoveToOtherPeoplesNote() {
            NoteMotion motion = new NoteMotion(note1, false);
            assertThrows(NoAccessRightException.class, () ->
                    controller.moveNote(note2, motion)
            );
        }
    }

}

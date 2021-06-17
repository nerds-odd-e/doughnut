package com.odde.doughnut.controllers;

import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.NoteContent;
import com.odde.doughnut.entities.NoteMotion;
import com.odde.doughnut.entities.User;
import com.odde.doughnut.entities.json.NoteViewedByUser;
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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = {"classpath:repository.xml"})
@Transactional
class RestNoteControllerTests {
    @Autowired
    ModelFactoryService modelFactoryService;

    @Autowired
    MakeMe makeMe;
    private UserModel userModel;
    RestNoteController controller;

    @BeforeEach
    void setup() {
        userModel = makeMe.aUser().toModelPlease();
        controller = new RestNoteController(modelFactoryService, new TestCurrentUserFetcher(userModel));
    }

    @Nested
    class showNoteTest {
        @Test
        void shouldNotBeAbleToSeeNoteIDontHaveAccessTo() {
            User otherUser = makeMe.aUser().please();
            Note note = makeMe.aNote().byUser(otherUser).please();
            assertThrows(NoAccessRightException.class, () -> controller.show(note));
        }

        @Test
        void shouldReturnTheNoteInfoIfHavingReadingAuth() throws NoAccessRightException {
            User otherUser = makeMe.aUser().please();
            Note note = makeMe.aNote().byUser(otherUser).please();
            makeMe.aBazaarNodebook(note.getNotebook()).please();
            makeMe.refresh(userModel.getEntity());
            final NoteViewedByUser show = controller.show(note);
            assertThat(show.getNote(), equalTo(note));
            assertThat(show.getOwns(), is(false));
        }

        @Test
        void shouldBeAbleToSeeOwnNote() throws NoAccessRightException {
            Note note = makeMe.aNote().byUser(userModel).please();
            makeMe.refresh(userModel.getEntity());
            final NoteViewedByUser show = controller.show(note);
            assertThat(show.getNote(), equalTo(note));
            assertThat(show.getOwns(), is(true));
        }

    }

    @Nested
    class showStatistics {
        @Test
        void shouldNotBeAbleToSeeNoteIDontHaveAccessTo() {
            User otherUser = makeMe.aUser().please();
            Note note = makeMe.aNote().byUser(otherUser).please();
            assertThrows(NoAccessRightException.class, () -> controller.statistics(note));
        }

        @Test
        void shouldReturnTheNoteInfoIfHavingReadingAuth() throws NoAccessRightException {
            User otherUser = makeMe.aUser().please();
            Note note = makeMe.aNote().byUser(otherUser).please();
            makeMe.aSubscription().forUser(userModel.getEntity()).forNotebook(note.getNotebook()).please();
            makeMe.refresh(userModel.getEntity());
            assertThat(controller.statistics(note).getNote(), equalTo(note));
        }
    }
}

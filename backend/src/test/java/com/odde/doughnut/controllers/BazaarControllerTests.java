package com.odde.doughnut.controllers;

import com.odde.doughnut.exceptions.NoAccessRightException;
import com.odde.doughnut.entities.NoteEntity;
import com.odde.doughnut.entities.UserEntity;
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
import org.springframework.web.servlet.view.RedirectView;

import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = {"classpath:repository.xml"})
@Transactional
class BazaarControllerTests {
    @Autowired ModelFactoryService modelFactoryService;
    @Autowired private MakeMe makeMe;
    private UserModel userModel;
    private NoteEntity topNote;
    private BazaarController controller;
    final ExtendedModelMap model = new ExtendedModelMap();


    @BeforeEach
    void setup() {
        userModel = makeMe.aUser().toModelPlease();
        topNote = makeMe.aNote().forUser(userModel).please();
        controller = new BazaarController(new TestCurrentUserFetcher(userModel), modelFactoryService);
    }

    @Test
    void renderTheTemplate() {
        assertEquals("bazaar/index", controller.bazaar(model));
    }

    @Nested
    class ShareMyNote {
        @Test
        void shareMyNote() throws NoAccessRightException {
            long oldCount = modelFactoryService.bazaarNoteRepository.count();
            RedirectView rv = controller.shareNote(topNote);
            assertEquals("/notes", rv.getUrl());
            assertThat(modelFactoryService.bazaarNoteRepository.count(), equalTo(oldCount + 1));
        }

        @Test
        void shouldNotBeAbleToShareNoteThatBelongsToOtherUser() {
            UserEntity anotherUserEntity = makeMe.aUser().please();
            NoteEntity note = makeMe.aNote().forUser(anotherUserEntity).please();
            assertThrows(NoAccessRightException.class, ()->
                    controller.shareNote(note)
            );
        }

    }

}

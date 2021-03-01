package com.odde.doughnut.controllers;

import com.odde.doughnut.controllers.exceptions.NoAccessRightException;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.User;
import com.odde.doughnut.entities.repositories.BazaarNoteRepository;
import com.odde.doughnut.services.ModelFactoryService;
import com.odde.doughnut.testability.DBCleaner;
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

import javax.persistence.EntityManager;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = {"classpath:repository.xml"})
@ExtendWith(DBCleaner.class)
@Transactional
class BazaarControllerTests {
    @Autowired private BazaarNoteRepository bazaarRepository;
    @Autowired ModelFactoryService modelFactoryService;
    @Autowired EntityManager entityManager;
    private MakeMe makeMe;
    private User user;
    private Note topNote;
    private BazaarController controller;
    ExtendedModelMap model = new ExtendedModelMap();


    @BeforeEach
    void setup() {
        makeMe = new MakeMe();
        user = makeMe.aUser().please(modelFactoryService);
        topNote = makeMe.aNote().forUser(user).please(modelFactoryService);
        controller = new BazaarController(new TestCurrentUser(user), bazaarRepository, modelFactoryService);
    }

    @Test
    void whenThereIsNoSharedNote() {
        assertEquals("bazaar", controller.bazaar(model));
        assertThat((List<Note>) model.getAttribute("notes"), hasSize(equalTo(0)));
    }

    @Test
    void whenThereIsSharedNote() {
        makeMe.aBazaarNode(topNote).please(modelFactoryService);
        assertEquals("bazaar", controller.bazaar(model));
        assertThat((List<Note>) model.getAttribute("notes"), hasSize(equalTo(1)));
    }

    @Nested
    class ShareMyNote {
        @Test
        void shareMyNote() throws NoAccessRightException {
            long oldCount = bazaarRepository.count();
            RedirectView rv = controller.shareNote(topNote);
            assertEquals("/notes", rv.getUrl());
            assertThat(bazaarRepository.count(), equalTo(oldCount + 1));
        }

        @Test
        void shouldNotBeAbleToShareNoteThatBelongsToOtherUser() {
            User anotherUser = makeMe.aUser().please(modelFactoryService);
            Note note = makeMe.aNote().forUser(anotherUser).please(modelFactoryService);
            assertThrows(NoAccessRightException.class, ()->
                    controller.shareNote(note)
            );
        }

    }

}

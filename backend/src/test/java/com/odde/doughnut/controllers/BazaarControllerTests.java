package com.odde.doughnut.controllers;

import com.odde.doughnut.models.Note;
import com.odde.doughnut.models.User;
import com.odde.doughnut.repositories.BazaarNoteRepository;
import com.odde.doughnut.repositories.NoteRepository;
import com.odde.doughnut.repositories.UserRepository;
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

import javax.persistence.EntityManager;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = {"classpath:repository.xml"})
@ExtendWith(DBCleaner.class)
@Transactional
class BazaarControllerTests {
    @Autowired private NoteRepository noteRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private BazaarNoteRepository bazaarRepository;
    @Autowired EntityManager entityManager;
    private MakeMe makeMe;
    private User user;
    private Note topNote;
    private BazaarController controller;
    ExtendedModelMap model = new ExtendedModelMap();


    @BeforeEach
    void setup() {
        makeMe = new MakeMe();
        user = makeMe.aUser().please(userRepository);
        topNote = makeMe.aNote().forUser(user).please(noteRepository);
        controller = new BazaarController(new TestCurrentUser(user), noteRepository, bazaarRepository);
    }

    @Test
    void whenThereIsNoSharedNote() {
        assertEquals("bazaar", controller.bazaar(model));
        assertThat((List<Note>) model.getAttribute("notes"), hasSize(equalTo(0)));
    }

    @Test
    void whenThereIsSharedNote() {
        makeMe.aBazaarNode(topNote).please(bazaarRepository);
        assertEquals("bazaar", controller.bazaar(model));
        assertThat((List<Note>) model.getAttribute("notes"), hasSize(equalTo(1)));
    }

}

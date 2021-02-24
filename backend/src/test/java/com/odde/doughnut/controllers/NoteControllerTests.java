package com.odde.doughnut.controllers;

import com.odde.doughnut.models.Note;
import com.odde.doughnut.models.User;
import com.odde.doughnut.repositories.NoteRepository;
import com.odde.doughnut.repositories.UserRepository;
import com.odde.doughnut.testability.DBCleaner;
import com.odde.doughnut.testability.MakeMe;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Commit;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.ui.ExtendedModelMap;

import javax.persistence.EntityManager;
import java.util.List;
import java.util.Optional;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = {"classpath:repository.xml"})
@ExtendWith(DBCleaner.class)
@Transactional
class NoteControllerTests {
    @Autowired
    private NoteRepository noteRepository;
    @Autowired
    private UserRepository userRepository;
    private MakeMe makeMe;
    private User user;
    private Note parentNote;
    private Note childNote;
    private NoteRestController noteController;
    ExtendedModelMap model = new ExtendedModelMap();
    NoteController controller;

    @Autowired
    EntityManager entityManager;

    @BeforeEach
    void setup() {
        makeMe = new MakeMe();
        user = makeMe.aUser().please(userRepository);
        parentNote = makeMe.aNote().forUser(user).please(noteRepository);
        childNote = makeMe.aNote().forUser(user).under(parentNote).please(noteRepository);
        entityManager.refresh(user);
        controller = new NoteController(new TestCurrentUser(user), noteRepository);
    }

    @Test
    void shouldProceedToNotePageWhenUserIsLogIn() {
        assertEquals("note", controller.notes(model));
    }

    @Test
    void shouldUseAllMyNotesTemplate() {
        assertEquals("all_my_notes", controller.all_my_notes(null, model));
    }

    @Test
    void shouldReturnAllParentlessNoteIfNoNoteIdGiven() {
        controller.all_my_notes(null, model);
        assertThat(model.getAttribute("note"), is(nullValue()));
        assertThat((List<Note>) model.getAttribute("all_my_notes"), hasSize(equalTo(1)));
        assertThat((List<Note>) model.getAttribute("all_my_notes"), contains(parentNote));
    }

    @Test
    void shouldReturnChildNoteIfNoteIdGiven() {
        controller.all_my_notes(parentNote.getId(), model);
        assertThat(((Note) model.getAttribute("note")).getId(), equalTo(parentNote.getId()));
//    assertThat((List<Note>)model.getAttribute("all_my_notes"), hasSize(equalTo(1)));
//    assertThat((List<Note>)model.getAttribute("all_my_notes"), contains(childNote));
    }

}

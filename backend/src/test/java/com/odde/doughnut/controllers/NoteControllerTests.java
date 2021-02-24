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
        makeMe.refresh(entityManager, user);
        makeMe.refresh(entityManager, parentNote);
        controller = new NoteController(new TestCurrentUser(user), noteRepository);
    }

    @Test
    void shouldUseTheRigthTemplateForCreatingNote() {
        assertEquals("new_note", controller.newNote(null, model));
        assertThat(((Note)model.getAttribute("note")).getParentNote(), is(nullValue()));
    }

    @Test
    void shouldGetTheParentNoteIfIdProvided() {
        controller.newNote(parentNote.getId(), model);
        assertThat(((Note)model.getAttribute("note")).getParentNote(), equalTo(parentNote));
    }

    @Test
    void shouldReturnAllParentlessNotesForMyNotes() {
        assertEquals("my_notes", controller.myNotes( model));
        assertThat(model.getAttribute("note"), is(nullValue()));
        assertThat((List<Note>) model.getAttribute("notes"), hasSize(equalTo(1)));
        assertThat((List<Note>) model.getAttribute("notes"), contains(parentNote));
    }

    @Test
    void shouldReturnChildNoteIfNoteIdGiven() {
        assertEquals("note", controller.note(parentNote.getId(), model));
        assertThat(((Note) model.getAttribute("note")).getId(), equalTo(parentNote.getId()));
        assertThat((List<Note>) model.getAttribute("notes"), hasSize(equalTo(1)));
        assertThat(((List<Note>) model.getAttribute("notes")), contains(childNote));
    }
}

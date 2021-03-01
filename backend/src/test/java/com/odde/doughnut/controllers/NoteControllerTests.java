package com.odde.doughnut.controllers;

import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.User;
import com.odde.doughnut.entities.repositories.NoteRepository;
import com.odde.doughnut.entities.repositories.UserRepository;
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
import org.springframework.validation.BindingResult;

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
    ModelFactoryService modelFactoryService;

    private MakeMe makeMe = new MakeMe();
    private User user;
    private Note parentNote;
    private Note childNote;
    ExtendedModelMap model = new ExtendedModelMap();
    NoteController controller;

    @BeforeEach
    void setup() {
        user = makeMe.aUser().please(modelFactoryService);
        controller = new NoteController(new TestCurrentUser(user), modelFactoryService);
    }

    @Nested
    class GetNotes {
        @Autowired
        EntityManager entityManager;

        @BeforeEach
        void setup() {
            parentNote = makeMe.aNote().forUser(user).please(modelFactoryService);
            childNote = makeMe.aNote().forUser(user).under(parentNote).please(modelFactoryService);
            makeMe.refresh(entityManager, user);
            makeMe.refresh(entityManager, parentNote);
        }

        @Test
        void shouldUseTheRigthTemplateForCreatingNote() {
            assertEquals("new_note", controller.newNote(null, model));
            assertThat(((Note) model.getAttribute("note")).getParentNote(), is(nullValue()));
        }

        @Test
        void shouldGetTheParentNoteIfIdProvided() {
            controller.newNote(parentNote, model);
            assertThat(((Note) model.getAttribute("note")).getParentNote(), equalTo(parentNote));
        }

        @Test
        void shouldReturnAllParentlessNotesForMyNotes() {
            assertEquals("my_notes", controller.myNotes(model));
            assertThat(model.getAttribute("note"), is(nullValue()));
            assertThat((List<Note>) model.getAttribute("notes"), hasSize(equalTo(1)));
            assertThat((List<Note>) model.getAttribute("notes"), contains(parentNote));
        }

        @Test
        void shouldReturnChildNoteIfNoteIdGiven() {
            assertEquals("note", controller.note(parentNote, model));
            assertThat(((Note) model.getAttribute("note")).getId(), equalTo(parentNote.getId()));
        }
    }

    @Nested
    class createNoteTest {
        @Test
        void shouldBeAbleToSaveNoteWhenValid() {
            Note newNote = makeMe.aNote().inMemoryPlease();
            BindingResult bindingResult = makeMe.successfulBindingResult();

            String response = controller.createNote(newNote, bindingResult);
            assertEquals("redirect:/notes/" + newNote.getId(), response);
        }

        @Test
        void shouldNotBeAbleToSaveNoteWhenInvalid() {
            Note newNote = new Note();
            BindingResult bindingResult = makeMe.failedBindingResult();

            String response = controller.createNote(newNote, bindingResult);
            assertEquals(null, newNote.getId());
            assertEquals("new_note", response);
        }

    }

    @Nested
    class updateNoteTest {
        Note note;

        @BeforeEach
        void setup() {
            note = makeMe.aNote().please(modelFactoryService);
            note.setTitle("new");
        }

        @Test
        void shouldBeAbleToSaveNoteWhenValid() {
            BindingResult bindingResult = makeMe.successfulBindingResult();

            String response = controller.updateNote(note, bindingResult);
            assertEquals("redirect:/notes/" + note.getId(), response);
        }

        @Test
        void shouldNotBeAbleToSaveNoteWhenInvalid() {
            Note note = makeMe.aNote().please(modelFactoryService);
            note.setTitle("new");
            BindingResult bindingResult = makeMe.failedBindingResult();

            String response = controller.updateNote(note, bindingResult);
            assertEquals("edit_note", response);
        }
    }
}


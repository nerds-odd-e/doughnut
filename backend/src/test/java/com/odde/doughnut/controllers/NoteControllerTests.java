package com.odde.doughnut.controllers;

import com.odde.doughnut.controllers.exceptions.NoAccessRightException;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.User;
import com.odde.doughnut.entities.repositories.LinkRepository;
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
import org.springframework.web.servlet.view.RedirectView;

import javax.persistence.EntityManager;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertFalse;

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

    @Nested
    class DeleteNoteTest {
        @Autowired private LinkRepository linkRepository;
        @Autowired EntityManager entityManager;

        @Test
        void shouldNotBeAbleToDeleteNoteThatBelongsToOtherUser() {
            User anotherUser = makeMe.aUser().please(modelFactoryService);
            Note note = makeMe.aNote().forUser(anotherUser).please(modelFactoryService);
            Integer noteId = note.getId();
            assertThrows(NoAccessRightException.class, ()->
                    controller.deleteNote(note)
            );
            assertTrue(modelFactoryService.findNoteById(noteId).isPresent());
        }

        @Test
        void shouldDeleteTheNoteButNotTheUser() throws NoAccessRightException {
            Note note = makeMe.aNote().forUser(user).please(modelFactoryService);
            Integer noteId = note.getId();
            RedirectView response = controller.deleteNote(note);
            assertEquals("/notes", response.getUrl());
            assertFalse(modelFactoryService.findNoteById(noteId).isPresent());
            assert(modelFactoryService.findUserById(note.getId()).isPresent());
        }

        @Test
        void shouldDeleteTheChildNoteButNotSiblingOrParent() throws NoAccessRightException {
            Note parent = makeMe.aNote().forUser(user).please(modelFactoryService);
            Note subject = makeMe.aNote().under(parent).forUser(user).please(modelFactoryService);
            Note sibling = makeMe.aNote().under(parent).forUser(user).please(modelFactoryService);
            Note child = makeMe.aNote().under(subject).forUser(user).please(modelFactoryService);

            controller.deleteNote(subject);

            assertTrue(modelFactoryService.findNoteById(sibling.getId()).isPresent());
            assertFalse(modelFactoryService.findNoteById(child.getId()).isPresent());
        }

        @Test
        void shouldDeleteTheLinkToAndFromThisNote() throws NoAccessRightException {
            Note referTo = makeMe.aNote().forUser(user).please(modelFactoryService);
            Note subject = makeMe.aNote().forUser(user).linkTo(referTo).please(modelFactoryService);
            Note referFrom = makeMe.aNote().forUser(user).linkTo(subject).linkTo(referTo).please(modelFactoryService);
            long oldCount = linkRepository.count();

            controller.deleteNote(subject);

            assertThat(makeMe.refresh(entityManager, referFrom).getId(), is(not(nullValue())));
            assertThat(makeMe.refresh(entityManager, referTo).getId(), is(not(nullValue())));
            assertThat(linkRepository.count(), equalTo(oldCount - 2));
        }
    }
}


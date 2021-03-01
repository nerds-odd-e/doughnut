package com.odde.doughnut.controllers;

import com.odde.doughnut.controllers.exceptions.NoAccessRightException;
import com.odde.doughnut.entities.NoteEntity;
import com.odde.doughnut.entities.UserEntity;
import com.odde.doughnut.entities.repositories.LinkRepository;
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
class NoteControllerTests {
    @Autowired
    ModelFactoryService modelFactoryService;

    private MakeMe makeMe = new MakeMe();
    private UserEntity userEntity;
    private NoteEntity parentNote;
    private NoteEntity childNote;
    ExtendedModelMap model = new ExtendedModelMap();
    NoteController controller;

    @BeforeEach
    void setup() {
        userEntity = makeMe.aUser().please(modelFactoryService);
        controller = new NoteController(new TestCurrentUser(userEntity), modelFactoryService);
    }

    @Nested
    class GetNotes {
        @Autowired
        EntityManager entityManager;

        @BeforeEach
        void setup() {
            parentNote = makeMe.aNote().forUser(userEntity).please(modelFactoryService);
            childNote = makeMe.aNote().forUser(userEntity).under(parentNote).please(modelFactoryService);
            makeMe.refresh(entityManager, userEntity);
            makeMe.refresh(entityManager, parentNote);
        }

        @Test
        void shouldUseTheRigthTemplateForCreatingNote() {
            assertEquals("new_note", controller.newNote(null, model));
            assertThat(((NoteEntity) model.getAttribute("noteEntity")).getParentNote(), is(nullValue()));
        }

        @Test
        void shouldGetTheParentNoteIfIdProvided() {
            controller.newNote(parentNote, model);
            assertThat(((NoteEntity) model.getAttribute("noteEntity")).getParentNote(), equalTo(parentNote));
        }

        @Test
        void shouldReturnAllParentlessNotesForMyNotes() {
            assertEquals("my_notes", controller.myNotes(model));
            assertThat(model.getAttribute("note"), is(nullValue()));
            assertThat((List<NoteEntity>) model.getAttribute("notes"), hasSize(equalTo(1)));
            assertThat((List<NoteEntity>) model.getAttribute("notes"), contains(parentNote));
        }

        @Test
        void shouldReturnChildNoteIfNoteIdGiven() {
            assertEquals("note", controller.note(parentNote, model));
            assertThat(((NoteEntity) model.getAttribute("note")).getId(), equalTo(parentNote.getId()));
        }
    }

    @Nested
    class createNoteTest {
        @Test
        void shouldBeAbleToSaveNoteWhenValid() {
            NoteEntity newNote = makeMe.aNote().inMemoryPlease();
            BindingResult bindingResult = makeMe.successfulBindingResult();

            String response = controller.createNote(newNote, bindingResult, model);
            assertEquals("redirect:/notes/" + newNote.getId(), response);
        }

        @Test
        void shouldNotBeAbleToSaveNoteWhenInvalid() {
            NoteEntity newNote = new NoteEntity();
            BindingResult bindingResult = makeMe.failedBindingResult();

            String response = controller.createNote(newNote, bindingResult, model);
            assertEquals(null, newNote.getId());
            assertEquals("new_note", response);
        }

    }

    @Nested
    class updateNoteTest {
        NoteEntity note;

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
            NoteEntity note = makeMe.aNote().please(modelFactoryService);
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
            UserEntity anotherUserEntity = makeMe.aUser().please(modelFactoryService);
            NoteEntity note = makeMe.aNote().forUser(anotherUserEntity).please(modelFactoryService);
            Integer noteId = note.getId();
            assertThrows(NoAccessRightException.class, ()->
                    controller.deleteNote(note)
            );
            assertTrue(modelFactoryService.findNoteById(noteId).isPresent());
        }

        @Test
        void shouldDeleteTheNoteButNotTheUser() throws NoAccessRightException {
            NoteEntity note = makeMe.aNote().forUser(userEntity).please(modelFactoryService);
            Integer noteId = note.getId();
            RedirectView response = controller.deleteNote(note);
            assertEquals("/notes", response.getUrl());
            assertFalse(modelFactoryService.findNoteById(noteId).isPresent());
            assertTrue(modelFactoryService.findUserById(userEntity.getId()).isPresent());
        }

        @Test
        void shouldDeleteTheChildNoteButNotSiblingOrParent() throws NoAccessRightException {
            NoteEntity parent = makeMe.aNote().forUser(userEntity).please(modelFactoryService);
            NoteEntity subject = makeMe.aNote().under(parent).forUser(userEntity).please(modelFactoryService);
            NoteEntity sibling = makeMe.aNote().under(parent).forUser(userEntity).please(modelFactoryService);
            NoteEntity child = makeMe.aNote().under(subject).forUser(userEntity).please(modelFactoryService);

            controller.deleteNote(subject);

            assertTrue(modelFactoryService.findNoteById(sibling.getId()).isPresent());
            assertFalse(modelFactoryService.findNoteById(child.getId()).isPresent());
        }

        @Test
        void shouldDeleteTheLinkToAndFromThisNote() throws NoAccessRightException {
            NoteEntity referTo = makeMe.aNote().forUser(userEntity).please(modelFactoryService);
            NoteEntity subject = makeMe.aNote().forUser(userEntity).linkTo(referTo).please(modelFactoryService);
            NoteEntity referFrom = makeMe.aNote().forUser(userEntity).linkTo(subject).linkTo(referTo).please(modelFactoryService);
            long oldCount = linkRepository.count();

            controller.deleteNote(subject);

            assertThat(makeMe.refresh(entityManager, referFrom).getId(), is(not(nullValue())));
            assertThat(makeMe.refresh(entityManager, referTo).getId(), is(not(nullValue())));
            assertThat(linkRepository.count(), equalTo(oldCount - 2));
        }
    }
}


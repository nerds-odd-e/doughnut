package com.odde.doughnut.controllers;

import com.odde.doughnut.entities.NoteMotionEntity;
import com.odde.doughnut.exceptions.NoAccessRightException;
import com.odde.doughnut.entities.NoteEntity;
import com.odde.doughnut.entities.UserEntity;
import com.odde.doughnut.entities.repositories.LinkRepository;
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
import org.springframework.validation.BindingResult;
import org.springframework.web.servlet.view.RedirectView;

import javax.persistence.EntityManager;
import java.io.IOException;
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

    @Autowired MakeMe makeMe;
    private UserModel userModel;
    private NoteEntity parentNote;
    final ExtendedModelMap model = new ExtendedModelMap();
    NoteController controller;

    @BeforeEach
    void setup() {
        userModel = makeMe.aUser().toModelPlease();
        controller = new NoteController(new TestCurrentUserFetcher(userModel), modelFactoryService);
    }

    @Nested
    class createNoteTest {
        @Test
        void shouldBeAbleToSaveNoteWhenValid() throws NoAccessRightException, IOException {
            NoteEntity newNote = makeMe.aNote().byUser(userModel).inMemoryPlease();
            BindingResult bindingResult = makeMe.successfulBindingResult();

            String response = controller.createNote(userModel.getOwnershipModel().getEntity(), null, newNote, bindingResult, model);
            assertEquals("redirect:/notes/" + newNote.getId(), response);
        }

        @Test
        void shouldNotBeAbleToSaveNoteWhenInvalid() throws NoAccessRightException, IOException {
            NoteEntity newNote = new NoteEntity();
            BindingResult bindingResult = makeMe.failedBindingResult();

            String response = controller.createNote(userModel.getOwnershipModel().getEntity(), null, newNote, bindingResult, model);
            assertNull(newNote.getId());
            assertEquals("notes/new", response);
        }

    }

    @Nested
    class updateNoteTest {
        NoteEntity note;

        @BeforeEach
        void setup() {
            note = makeMe.aNote("new").byUser(userModel).please();
        }

        @Test
        void shouldBeAbleToSaveNoteWhenValid() throws NoAccessRightException, IOException {
            BindingResult bindingResult = makeMe.successfulBindingResult();
            String response = controller.updateNote(note, bindingResult);
            assertEquals("redirect:/notes/" + note.getId(), response);
        }

        @Test
        void shouldNotBeAbleToSaveNoteWhenInvalid() throws NoAccessRightException, IOException {
            BindingResult bindingResult = makeMe.failedBindingResult();
            String response = controller.updateNote(note, bindingResult);
            assertEquals("notes/edit", response);
        }

        @Test
        void shouldAddUploadedPicture() throws NoAccessRightException, IOException {
            makeMe.theNote(note).withNewlyUploadedPicture();
            BindingResult bindingResult = makeMe.successfulBindingResult();
            String response = controller.updateNote(note, bindingResult);
            assertThat(note.getNoteContent().getUploadPicture(), is(not(nullValue())));
        }

    }

    @Nested
    class DeleteNoteTest {
        @Autowired
        private LinkRepository linkRepository;
        @Autowired
        EntityManager entityManager;

        @Test
        void shouldNotBeAbleToDeleteNoteThatBelongsToOtherUser() {
            UserEntity anotherUserEntity = makeMe.aUser().please();
            NoteEntity note = makeMe.aNote().byUser(anotherUserEntity).please();
            Integer noteId = note.getId();
            assertThrows(NoAccessRightException.class, () ->
                    controller.deleteNote(note)
            );
            assertTrue(modelFactoryService.findNoteById(noteId).isPresent());
        }

        @Test
        void shouldDeleteTheNoteButNotTheUser() throws NoAccessRightException {
            NoteEntity note = makeMe.aNote().byUser(userModel).please();
            Integer noteId = note.getId();
            RedirectView response = controller.deleteNote(note);
            assertEquals("/notes", response.getUrl());
            assertFalse(modelFactoryService.findNoteById(noteId).isPresent());
            assertTrue(modelFactoryService.findUserById(userModel.getEntity().getId()).isPresent());
        }

        @Test
        void shouldDeleteTheChildNoteButNotSiblingOrParent() throws NoAccessRightException {
            NoteEntity parent = makeMe.aNote().byUser(userModel).please();
            NoteEntity subject = makeMe.aNote().under(parent).byUser(userModel).please();
            NoteEntity sibling = makeMe.aNote().under(parent).byUser(userModel).please();
            NoteEntity child = makeMe.aNote().under(subject).byUser(userModel).please();
            makeMe.refresh(subject);

            controller.deleteNote(subject);

            assertTrue(modelFactoryService.findNoteById(sibling.getId()).isPresent());
            assertFalse(modelFactoryService.findNoteById(child.getId()).isPresent());
        }

        @Test
        void shouldDeleteTheReviewPoints() throws NoAccessRightException {
            NoteEntity subject = makeMe.aNote().byUser(userModel).please();
            NoteEntity child = makeMe.aNote().byUser(userModel).under(subject).please();
            makeMe.aReviewPointFor(child).by(userModel).please();
            long oldCount = makeMe.modelFactoryService.reviewPointRepository.count();
            makeMe.refresh(subject);
            controller.deleteNote(subject);

            assertThat(makeMe.modelFactoryService.reviewPointRepository.count(), equalTo(oldCount - 1));
        }

    }

    @Nested
    class MoveNoteTest {
        UserEntity anotherUser;
        NoteEntity note1;
        NoteEntity note2;

        @BeforeEach
        void setup() {
            anotherUser = makeMe.aUser().please();
            note1 = makeMe.aNote().byUser(anotherUser).please();
            note2 = makeMe.aNote().byUser(userModel).please();
        }

        @Test
        void shouldNotAllowMoveOtherPeoplesNote() {
            NoteMotionEntity motion = new NoteMotionEntity(note2, false);
            assertThrows(NoAccessRightException.class, ()->
                    controller.moveNote(note1, motion)
            );
        }

        @Test
        void shouldNotAllowMoveToOtherPeoplesNote() {
            NoteMotionEntity motion = new NoteMotionEntity(note1, false);
            assertThrows(NoAccessRightException.class, ()->
                    controller.moveNote(note2, motion)
            );
        }

    }

}

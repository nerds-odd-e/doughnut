package com.odde.doughnut.controllers;

import com.odde.doughnut.entities.*;
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
import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.Locale;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = {"classpath:repository.xml"})
@Transactional
class NoteControllerTests {
    @Autowired
    ModelFactoryService modelFactoryService;

    @Autowired
    MakeMe makeMe;
    private UserModel userModel;
    private Note parentNote;
    final ExtendedModelMap model = new ExtendedModelMap();
    NoteController controller;

    @BeforeEach
    void setup() {
        userModel = makeMe.aUser().toModelPlease();
        controller = new NoteController(new TestCurrentUserFetcher(userModel), modelFactoryService);
    }

    @Nested
    class showNoteTest {

        @Test
        void shouldNotBeAbleToSeeNoteIDontHaveAccessTo() {
            User otherUser = makeMe.aUser().please();
            Note note = makeMe.aNote().byUser(otherUser).please();
            assertThrows(NoAccessRightException.class, () -> controller.showNote(note));
        }

        @Test
        void shouldRedirectToBazaarIfIHaveReadonlyAccess() throws NoAccessRightException {
            User otherUser = makeMe.aUser().please();
            Note note = makeMe.aNote().byUser(otherUser).please();
            makeMe.aSubscription().forUser(userModel.getEntity()).forNotebook(note.getNotebook()).please();
            makeMe.refresh(userModel.getEntity());
            assertThat(controller.showNote(note), equalTo("redirect:/bazaar/notes/" + note.getId()));
        }
    }

    @Nested
    class createNoteTest {
        @Test
        void shouldBeAbleToSaveNoteWhenValid() throws NoAccessRightException, IOException {
            Note parent = makeMe.aNote().byUser(userModel).please();
            Note newNote = makeMe.aNote().inMemoryPlease();
            BindingResult bindingResult = makeMe.successfulBindingResult();

            String response = controller.createNote(parent, newNote.getNoteContent(), bindingResult, model);
            assertThat(response, matchesPattern("redirect:/notes/\\d+"));
        }

        @Test
        void shouldNotBeAbleToSaveNoteWhenInvalid() throws NoAccessRightException, IOException {
            Note newNote = new Note();
            BindingResult bindingResult = makeMe.failedBindingResult();

            String response = controller.createNote(null, newNote.getNoteContent(), bindingResult, model);
            assertNull(newNote.getId());
            assertEquals("notes/new", response);
        }

        @Test
        void shouldCreateDateNotesWhenNotebookIsABlog() throws NoAccessRightException, IOException {
            Note blog = makeMe.aBlog("This is a blog").byUser(userModel).inBlog(new Notebook(NotebookType.BLOG)).please();
            BindingResult bindingResult = makeMe.successfulBindingResult();
            String response = controller.createNote(blog, blog.getNoteContent(), bindingResult, model);

            String[] split = response.split("/");
            int id = Integer.parseInt(split[split.length-1]);
            Note createdArticle = modelFactoryService.findNoteById(id).stream().findFirst().orElse(null);
            assertNotNull(createdArticle);

            LocalDate d = LocalDate.now();
            String y = String.valueOf(d.getYear());
            String day = String.valueOf(d.getDayOfMonth());
            String m = d.getMonth().getDisplayName(TextStyle.SHORT, Locale.ENGLISH);

            Note dateNote = createdArticle.getParentNote();
            assertNotNull(dateNote);
            assertEquals(day, dateNote.getTitle());

            Note monthNote = dateNote.getParentNote();
            assertNotNull(monthNote);
            assertEquals(m, monthNote.getTitle());

            Note yearNote = monthNote.getParentNote();
            assertNotNull(yearNote);
            assertEquals(y, yearNote.getTitle());

        }

    }

    @Nested
    class updateNoteTest {
        Note note;

        @BeforeEach
        void setup() {
            note = makeMe.aNote("new").byUser(userModel).please();
        }

        @Test
        void shouldBeAbleToSaveNoteWhenValid() throws NoAccessRightException, IOException {
            BindingResult bindingResult = makeMe.successfulBindingResult();
            String response = controller.updateNote(note, note.getNoteContent(), bindingResult);
            assertEquals("redirect:/notes/" + note.getId(), response);
        }

        @Test
        void shouldNotBeAbleToSaveNoteWhenInvalid() throws NoAccessRightException, IOException {
            BindingResult bindingResult = makeMe.failedBindingResult();
            String response = controller.updateNote(note, note.getNoteContent(), bindingResult);
            assertEquals("notes/edit", response);
        }

        @Test
        void shouldAddUploadedPicture() throws NoAccessRightException, IOException {
            makeMe.theNote(note).withNewlyUploadedPicture();
            BindingResult bindingResult = makeMe.successfulBindingResult();
            controller.updateNote(note, note.getNoteContent(), bindingResult);
            assertThat(note.getNoteContent().getUploadPicture(), is(not(nullValue())));
        }

        @Test
        void shouldNotRemoveThePictureIfNoNewPictureInTheUpdate() throws NoAccessRightException, IOException {
            makeMe.theNote(note).withUploadedPicture();
            NoteContent newContent = makeMe.aNote().inMemoryPlease().getNoteContent();
            controller.updateNote(note, newContent, makeMe.successfulBindingResult());
            assertThat(note.getNoteContent().getUploadPicture(), is(not(nullValue())));
        }

    }

    @Nested
    class DeleteNoteTest {
        @Test
        void shouldNotBeAbleToDeleteNoteThatBelongsToOtherUser() {
            User anotherUser = makeMe.aUser().please();
            Note note = makeMe.aNote().byUser(anotherUser).please();
            Integer noteId = note.getId();
            assertThrows(NoAccessRightException.class, () ->
                    controller.deleteNote(note)
            );
            assertTrue(modelFactoryService.findNoteById(noteId).isPresent());
        }

        @Test
        void shouldDeleteTheNoteButNotTheUser() throws NoAccessRightException {
            Note note = makeMe.aNote().byUser(userModel).please();
            Integer noteId = note.getId();
            RedirectView response = controller.deleteNote(note);
            assertEquals("/notebooks", response.getUrl());
            assertFalse(modelFactoryService.findNoteById(noteId).isPresent());
            assertTrue(modelFactoryService.findUserById(userModel.getEntity().getId()).isPresent());
        }

        @Test
        void shouldDeleteTheChildNoteButNotSiblingOrParent() throws NoAccessRightException {
            Note parent = makeMe.aNote().byUser(userModel).please();
            Note subject = makeMe.aNote().under(parent).byUser(userModel).please();
            Note sibling = makeMe.aNote().under(parent).byUser(userModel).please();
            Note child = makeMe.aNote().under(subject).byUser(userModel).please();
            makeMe.refresh(subject);

            controller.deleteNote(subject);

            assertTrue(modelFactoryService.findNoteById(sibling.getId()).isPresent());
            assertFalse(modelFactoryService.findNoteById(child.getId()).isPresent());
        }

        @Test
        void shouldDeleteTheReviewPoints() throws NoAccessRightException {
            Note subject = makeMe.aNote().byUser(userModel).please();
            Note child = makeMe.aNote().byUser(userModel).under(subject).please();
            makeMe.aReviewPointFor(child).by(userModel).please();
            long oldCount = makeMe.modelFactoryService.reviewPointRepository.count();
            makeMe.refresh(subject);
            controller.deleteNote(subject);

            assertThat(makeMe.modelFactoryService.reviewPointRepository.count(), equalTo(oldCount - 1));
        }

    }

    @Nested
    class MoveNoteTest {
        User anotherUser;
        Note note1;
        Note note2;

        @BeforeEach
        void setup() {
            anotherUser = makeMe.aUser().please();
            note1 = makeMe.aNote().byUser(anotherUser).please();
            note2 = makeMe.aNote().byUser(userModel).please();
        }

        @Test
        void shouldNotAllowMoveOtherPeoplesNote() {
            NoteMotion motion = new NoteMotion(note2, false);
            assertThrows(NoAccessRightException.class, () ->
                    controller.moveNote(note1, motion)
            );
        }

        @Test
        void shouldNotAllowMoveToOtherPeoplesNote() {
            NoteMotion motion = new NoteMotion(note1, false);
            assertThrows(NoAccessRightException.class, () ->
                    controller.moveNote(note2, motion)
            );
        }

    }

}

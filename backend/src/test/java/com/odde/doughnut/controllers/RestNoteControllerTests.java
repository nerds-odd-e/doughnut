package com.odde.doughnut.controllers;

import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.NoteContent;
import com.odde.doughnut.entities.User;
import com.odde.doughnut.entities.json.NoteViewedByUser;
import com.odde.doughnut.entities.json.RedirectToNoteResponse;
import com.odde.doughnut.exceptions.NoAccessRightException;
import com.odde.doughnut.factoryServices.ModelFactoryService;
import com.odde.doughnut.models.UserModel;
import com.odde.doughnut.testability.MakeMe;
import com.odde.doughnut.testability.TestabilitySettings;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.sql.Timestamp;
import java.util.ArrayList;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = {"classpath:repository.xml"})
@Transactional
class RestNoteControllerTests {
    @Autowired
    ModelFactoryService modelFactoryService;

    @Autowired
    MakeMe makeMe;
    private UserModel userModel;
    RestNoteController controller;
    private TestabilitySettings testabilitySettings = new TestabilitySettings();

    @BeforeEach
    void setup() {
        userModel = makeMe.aUser().toModelPlease();
        controller = new RestNoteController(modelFactoryService, new TestCurrentUserFetcher(userModel), testabilitySettings);
    }

    @Nested
    class showNoteTest {
        @Test
        void shouldNotBeAbleToSeeNoteIDontHaveAccessTo() {
            User otherUser = makeMe.aUser().please();
            Note note = makeMe.aNote().byUser(otherUser).please();
            assertThrows(NoAccessRightException.class, () -> controller.show(note));
        }

        @Test
        void shouldReturnTheNoteInfoIfHavingReadingAuth() throws NoAccessRightException {
            User otherUser = makeMe.aUser().please();
            Note note = makeMe.aNote().byUser(otherUser).please();
            makeMe.aBazaarNodebook(note.getNotebook()).please();
            makeMe.refresh(userModel.getEntity());
            final NoteViewedByUser show = controller.show(note);
            assertThat(show.getNote(), equalTo(note));
            assertThat(show.getOwns(), is(false));
        }

        @Test
        void shouldBeAbleToSeeOwnNote() throws NoAccessRightException {
            Note note = makeMe.aNote().byUser(userModel).please();
            makeMe.refresh(userModel.getEntity());
            final NoteViewedByUser show = controller.show(note);
            assertThat(show.getNote(), equalTo(note));
            assertThat(show.getOwns(), is(true));
        }
        
        void shouldBeAbleToSeeOwnNoteOverview() throws NoAccessRightException {
            Note note = makeMe.aNote().byUser(userModel).please();
            Note childNote = makeMe.aNote().byUser(userModel).under(note).please();
            makeMe.theNote(childNote).with10Children().please();
            makeMe.refresh(note);
            makeMe.refresh(childNote);
            Note grandchildNote = childNote.getChildren().get(0);
            final ArrayList<NoteViewedByUser> showOverview = controller.showOverview(note);

            assertThat(showOverview.get(0).getNote(), equalTo(note));
            assertThat(showOverview.get(1).getNote(), equalTo(childNote));
            assertThat(showOverview.get(2).getNote(), equalTo(grandchildNote));
        }

        @Test
        void shouldShowNoteThatIsRecentlyUpdated() throws NoAccessRightException {
            Note note = makeMe.aNote().byUser(userModel).please();
            makeMe.refresh(userModel.getEntity());
            final NoteViewedByUser show = controller.show(note);
            assertThat(show.getIsRecentlyUpdated(testabilitySettings.getCurrentUTCTimestamp()), is(true));
        }

        @Test
        void shouldShowNoteThatIsNotRecentlyUpdated() throws NoAccessRightException {
            Timestamp twelveHoursAgo = new Timestamp(System.currentTimeMillis() - NoteViewedByUser.TWELVE_HOURS_MILLISECONDS);
            Note note = makeMe.aNote().byUser(userModel).withContentUpdatedAt(twelveHoursAgo).please();
            makeMe.refresh(userModel.getEntity());
            final NoteViewedByUser show = controller.show(note);
            assertThat(show.getIsRecentlyUpdated(testabilitySettings.getCurrentUTCTimestamp()), is(false));
        }
    }

    @Nested
    class showStatistics {
        @Test
        void shouldNotBeAbleToSeeNoteIDontHaveAccessTo() {
            User otherUser = makeMe.aUser().please();
            Note note = makeMe.aNote().byUser(otherUser).please();
            assertThrows(NoAccessRightException.class, () -> controller.statistics(note));
        }

        @Test
        void shouldReturnTheNoteInfoIfHavingReadingAuth() throws NoAccessRightException {
            User otherUser = makeMe.aUser().please();
            Note note = makeMe.aNote().byUser(otherUser).please();
            makeMe.aSubscription().forUser(userModel.getEntity()).forNotebook(note.getNotebook()).please();
            makeMe.refresh(userModel.getEntity());
            assertThat(controller.statistics(note).getNote(), equalTo(note));
        }
    }

    @Nested
    class createNoteTest {
        @Test
        void shouldBeAbleToSaveNoteWhenValid() throws NoAccessRightException, IOException {
            Note parent = makeMe.aNote().byUser(userModel).please();
            Note newNote = makeMe.aNote().inMemoryPlease();
            RestNoteController.NoteCreation noteCreation = new RestNoteController.NoteCreation();
            noteCreation.setNoteContent(newNote.getNoteContent());

            RedirectToNoteResponse response = controller.createNote(parent, noteCreation);
            assertThat(response.noteId, notNullValue());
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
            RedirectToNoteResponse response = controller.updateNote(note, note.getNoteContent());
            assertThat(response.noteId, equalTo(note.getId()));
        }

        @Test
        void shouldAddUploadedPicture() throws NoAccessRightException, IOException {
            makeMe.theNote(note).withNewlyUploadedPicture();
            controller.updateNote(note, note.getNoteContent());
            assertThat(note.getNoteContent().getUploadPicture(), is(not(nullValue())));
        }

        @Test
        void shouldNotRemoveThePictureIfNoNewPictureInTheUpdate() throws NoAccessRightException, IOException {
            makeMe.theNote(note).withUploadedPicture();
            NoteContent newContent = makeMe.aNote().inMemoryPlease().getNoteContent();
            controller.updateNote(note, newContent);
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
            RedirectToNoteResponse response = controller.deleteNote(note);
            assertEquals(null, response.noteId);
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


}

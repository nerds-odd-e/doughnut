package com.odde.doughnut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.List;

import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.NoteContent;
import com.odde.doughnut.entities.User;
import com.odde.doughnut.entities.json.NoteViewedByUser;
import com.odde.doughnut.entities.json.NotesBulk;
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
    private final TestabilitySettings testabilitySettings = new TestabilitySettings();

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
            final NotesBulk show = controller.show(note);
            assertThat(show.notes.get(0).getTitle(), equalTo(note.getTitle()));
            assertThat(show.notePosition.getOwns(), is(false));
        }

        @Test
        void shouldBeAbleToSeeOwnNote() throws NoAccessRightException {
            Note note = makeMe.aNote().byUser(userModel).please();
            makeMe.refresh(userModel.getEntity());
            final NotesBulk show = controller.show(note);
            assertThat(show.notes.get(0).getId(), equalTo(note.getId()));
            assertThat(show.notePosition.getOwns(), is(true));
        }

        @Test
        void shouldBeAbleToSeeOwnNoteOverview() throws NoAccessRightException {
            Note note = makeMe.aNote().byUser(userModel).please();
            Note childNote = makeMe.aNote().byUser(userModel).under(note).please();
            makeMe.theNote(childNote).with10Children().please();
            makeMe.refresh(note);
            makeMe.refresh(childNote);
            final NotesBulk showOverview = controller.showOverview(note);
            assertThat(showOverview.notes, hasSize(12));
            assertThat(showOverview.notePosition.getOwns(), equalTo(true));
            assertThat(showOverview.notes.get(0).getChildrenIds(), hasSize(1));
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
        void shouldBeAbleToSaveNoteWhenValid() throws NoAccessRightException {
            Note parent = makeMe.aNote().byUser(userModel).please();
            Note newNote = makeMe.aNote().inMemoryPlease();
            RestNoteController.NoteCreation noteCreation = new RestNoteController.NoteCreation();
            noteCreation.setNoteContent(newNote.getNoteContent());

            NotesBulk response = controller.createNote(parent, noteCreation);
            assertThat(response.notes.get(0).getId(), equalTo(parent.getId()));
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
            NoteViewedByUser response = controller.updateNote(note, note.getNoteContent());
            assertThat(response.getId(), equalTo(note.getId()));
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

        @Test
        void shouldNotBeAbleToSaveNoteWhenConflictAndReturnConflictNote() throws NoAccessRightException, IOException {
            Note newNote = makeMe.aNote().byUser(userModel).please();

            NoteContent modifiedContent = newNote.getNoteContent();
            modifiedContent.setTitle("modified Title");
            modifiedContent.setDescription("modified Description");
            controller.updateNote(newNote, modifiedContent);

            NoteContent newContent = makeMe.aNote().inMemoryPlease().getNoteContent();
            newContent.setTitle("Avengers");
            NoteViewedByUser response = controller.updateNote(newNote, newContent);

            assertThat(newNote.getNoteContent().getTitle(), equalTo(modifiedContent.getTitle()));
            assertThat(response.isConflicting(), equalTo(true));
            assertThat(response.getConflictingNoteContent().getTitle(), equalTo(newContent.getTitle()));
            assertThat(response.getConflictingNoteContent().getDescription(), equalTo(newContent.getDescription()));
        }

        @Test
        void shouldSetTranslationOutdatedTagWhenUpdatingEnglishNoteAndIndonesianTranslationIsAvailable() throws NoAccessRightException, IOException {
            note.getNoteContent().setTitleIDN("indonesian");
            NoteViewedByUser response = controller.updateNote(note, note.getNoteContent());

            assertEquals(true, response.getNoteContent().getIsTranslationOutdatedIDN());
        }

        @Test
        void shouldNotSetTranslationOutdatedTagWhenUpdatingEnglishNoteAndIndonesianTranslationIsNotAvailable() throws NoAccessRightException, IOException {
            note.getNoteContent().setTitleIDN(null);
            NoteContent newContent = note.getNoteContent();

            NoteViewedByUser response = controller.updateNote(note, newContent);

            assertEquals(note.getNoteContent().getIsTranslationOutdatedIDN(), response.getNoteContent().getIsTranslationOutdatedIDN());
        }

        @Test
        void shouldNotSetTranslationOutdatedTagWhenUpdatingIndonesianTranslation() throws NoAccessRightException, IOException {
            note.getNoteContent().setTitleIDN("indonesia");
            NoteContent newContent = makeMe.aNote().inMemoryPlease().getNoteContent();
            newContent.setTitleIDN("bahasa");

            NoteViewedByUser response = controller.updateNote(note, newContent);

            assertEquals(note.getNoteContent().getIsTranslationOutdatedIDN(), response.getNoteContent().getIsTranslationOutdatedIDN());
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
            Integer response = controller.deleteNote(note);
            assertEquals(noteId, response);
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
    class SplitNoteTest {
        @Test
        void shouldDoNothingWhenSplittingNodesWithoutDescription() throws NoAccessRightException {
            Note note = makeMe.aNote().withNoDescription().byUser(userModel).please();
            List<Note> childrenBefore =  note.getChildren();
            controller.splitNote(note);
            makeMe.refresh(note);
            List<Note> childrenAfter =  note.getChildren();
            assertEquals(childrenBefore.size(), childrenAfter.size());

        }

        @Test
        void shouldGenerateASingleNodeWhenSplittingNodesWithOnlyOneParagraphDescription() throws NoAccessRightException {
            Note note = makeMe.aNote("noteTitle","Just one paragraph").byUser(userModel).please();
            List<Note> childrenBefore =  note.getChildren();
            controller.splitNote(note);
            makeMe.refresh(note);
            List<Note> childrenAfter =  note.getChildren();
            assertEquals(0,childrenBefore.size());
            assertEquals(1,childrenAfter.size());

        }

        @Test
        void shouldGenerateASingleNodeWhenSplittingANodeWithAMultilineDescription() throws NoAccessRightException {
            Note note = makeMe.aNote("noteTitle","ThisIsTheTitle\nThisIsTheDescription").byUser(userModel).please();
            controller.splitNote(note);
            makeMe.refresh(note);
            List<Note> childrenAfter =  note.getChildren();
            assertEquals(1, childrenAfter.size());

            Note onlyChild = childrenAfter.get(0);
            assertEquals("ThisIsTheTitle", onlyChild.getTitle());
            assertEquals("ThisIsTheDescription", onlyChild.getShortDescription());
        }

        @Test
        void shouldGenerateMultipleNodeWhenSplittingANodeWithAMultipleParagraphs() throws NoAccessRightException {
            Note note = makeMe.aNote("noteTitle","ThisIsTheTitle1\nThisIsTheDescription1\n\nThisIsTheTitle2\nThisIsTheDescription2\nThisIsTheDescription21").byUser(userModel).please();
            controller.splitNote(note);
            makeMe.refresh(note);
            List<Note> childrenAfter =  note.getChildren();
            assertEquals(2, childrenAfter.size());

            Note child1 = childrenAfter.get(0);
            assertEquals("ThisIsTheTitle1", child1.getTitle());
            assertEquals("ThisIsTheDescription1", child1.getShortDescription());

            Note child2 = childrenAfter.get(1);
            assertEquals("ThisIsTheTitle2", child2.getTitle());
            assertEquals("ThisIsTheDescription2\nThisIsTheDescription21", child2.getShortDescription());
        }

        @Test
        void shouldDeleteParentDescriptionWhenSplittingANoteSuccessfully() throws NoAccessRightException {
            Note note = makeMe.aNote("noteTitle","ThisIsTheTitle1\nThisIsTheDescription1\n\nThisIsTheTitle2\nThisIsTheDescription2\nThisIsTheDescription21").byUser(userModel).please();
            controller.splitNote(note);

            assertEquals("", note.getShortDescription());
        }

        @Test
        void shouldDoNothingWhenDescriptionIsEmptyLines() throws NoAccessRightException {
            Note note = makeMe.aNote("noteTitle","\n\n\n\n\n").byUser(userModel).please();
            controller.splitNote(note);
            makeMe.refresh(note);

            List<Note> childrenAfter =  note.getChildren();
            assertEquals(0, childrenAfter.size());
        }

    }

}

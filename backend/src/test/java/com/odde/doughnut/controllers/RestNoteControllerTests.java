package com.odde.doughnut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.odde.doughnut.entities.Comment;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.NoteAccessories;
import com.odde.doughnut.entities.User;
import com.odde.doughnut.entities.json.NoteCreation;
import com.odde.doughnut.entities.json.NoteRealm;
import com.odde.doughnut.entities.json.NotesBulk;
import com.odde.doughnut.exceptions.NoAccessRightException;
import com.odde.doughnut.factoryServices.ModelFactoryService;
import com.odde.doughnut.models.TimestampOperations;
import com.odde.doughnut.models.UserModel;
import com.odde.doughnut.testability.MakeMe;
import com.odde.doughnut.testability.TestabilitySettings;
import java.io.IOException;
import java.sql.Timestamp;
import java.util.List;
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
  @Autowired ModelFactoryService modelFactoryService;

  @Autowired MakeMe makeMe;
  private UserModel userModel;
  RestNoteController controller;
  private final TestabilitySettings testabilitySettings = new TestabilitySettings();

  @BeforeEach
  void setup() {
    userModel = makeMe.aUser().toModelPlease();
    controller =
        new RestNoteController(
            modelFactoryService, new TestCurrentUserFetcher(userModel), testabilitySettings);
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
      assertThat(show.notes.get(0).getNote().getTitle(), equalTo(note.getTitle()));
      assertThat(show.notePosition.getNotebook().getFromBazaar(), is(true));
    }

    @Test
    void shouldBeAbleToSeeOwnNote() throws NoAccessRightException {
      Note note = makeMe.aNote().byUser(userModel).please();
      makeMe.refresh(userModel.getEntity());
      final NotesBulk show = controller.show(note);
      assertThat(show.notes.get(0).getId(), equalTo(note.getId()));
      assertThat(show.notePosition.getNotebook().getFromBazaar(), is(false));
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
      assertThat(showOverview.notePosition.getNotebook().getFromBazaar(), equalTo(false));
      assertThat(showOverview.notes.get(0).getChildrenIds().get(), hasSize(1));
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
      makeMe
          .aSubscription()
          .forUser(userModel.getEntity())
          .forNotebook(note.getNotebook())
          .please();
      makeMe.refresh(userModel.getEntity());
      assertThat(controller.statistics(note).getNote().getId(), equalTo(note.getId()));
    }
  }

  @Nested
  class createNoteTest {
    @Test
    void shouldBeAbleToSaveNoteWhenValid() throws NoAccessRightException {
      Note parent = makeMe.aNote().byUser(userModel).please();
      Note newNote = makeMe.aNote().inMemoryPlease();
      NoteCreation noteCreation = new NoteCreation();
      noteCreation.setTextContent(newNote.getTextContent());

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
      NoteRealm response = controller.updateNote(note, note.getNoteAccessories());
      assertThat(response.getId(), equalTo(note.getId()));
    }

    @Test
    void shouldAddUploadedPicture() throws NoAccessRightException, IOException {
      makeMe.theNote(note).withNewlyUploadedPicture();
      controller.updateNote(note, note.getNoteAccessories());
      assertThat(note.getNoteAccessories().getUploadPicture(), is(not(nullValue())));
    }

    @Test
    void shouldNotRemoveThePictureIfNoNewPictureInTheUpdate()
        throws NoAccessRightException, IOException {
      makeMe.theNote(note).withUploadedPicture();
      NoteAccessories newContent = makeMe.aNote().inMemoryPlease().getNoteAccessories();
      controller.updateNote(note, newContent);
      assertThat(note.getNoteAccessories().getUploadPicture(), is(not(nullValue())));
    }
  }

  @Nested
  class DeleteNoteTest {
    Note subject;
    Note parent;
    Note child;

    @BeforeEach
    void setup() {
      parent = makeMe.aNote().byUser(userModel).please();
      subject = makeMe.aNote().under(parent).byUser(userModel).please();
      child = makeMe.aNote("child").under(subject).byUser(userModel).please();
      makeMe.refresh(subject);
    }

    @Test
    void shouldNotBeAbleToDeleteNoteThatBelongsToOtherUser() {
      User anotherUser = makeMe.aUser().please();
      Note note = makeMe.aNote().byUser(anotherUser).please();
      assertThrows(NoAccessRightException.class, () -> controller.deleteNote(note));
    }

    @Test
    void shouldDeleteTheNoteButNotTheUser() throws NoAccessRightException {
      controller.deleteNote(subject);
      makeMe.refresh(parent);
      assertThat(parent.getChildren(), hasSize(0));
      assertTrue(modelFactoryService.findUserById(userModel.getEntity().getId()).isPresent());
    }

    @Test
    void shouldDeleteTheChildNoteButNotSibling() throws NoAccessRightException {
      makeMe.aNote("silbling").under(parent).byUser(userModel).please();
      controller.deleteNote(subject);
      makeMe.refresh(parent);
      assertThat(parent.getChildren(), hasSize(1));
      assertThat(parent.getDescendantsInBreathFirstOrder(), hasSize(1));
    }

    @Nested
    class UndoDeleteNoteTest {
      @Test
      void shouldUndoDeleteTheNote() throws NoAccessRightException {
        controller.deleteNote(subject);
        makeMe.refresh(subject);
        controller.undoDeleteNote(subject);
        makeMe.refresh(parent);
        assertThat(parent.getChildren(), hasSize(1));
        assertThat(parent.getDescendantsInBreathFirstOrder(), hasSize(2));
      }

      @Test
      void shouldUndoOnlylastChange() throws NoAccessRightException {
        Timestamp timestamp = new Timestamp(System.currentTimeMillis());
        testabilitySettings.timeTravelTo(timestamp);
        controller.deleteNote(child);
        makeMe.refresh(subject);

        timestamp = TimestampOperations.addHoursToTimestamp(timestamp, 1);
        testabilitySettings.timeTravelTo(timestamp);
        controller.deleteNote(subject);
        makeMe.refresh(subject);

        controller.undoDeleteNote(subject);
        makeMe.refresh(parent);
        assertThat(parent.getDescendantsInBreathFirstOrder(), hasSize(1));
      }
    }
  }

  @Nested
  class CreateComment {

    @Test
    void thereShouldBeNoCommentBeforeCreate() {
      Note note = makeMe.aNote().please();
      List<Comment> comments = makeMe.modelFactoryService.commentRepository.findAllByNote(note);
      assertThat(comments, hasSize(0));
    }

    @Test
    void shouldCreateComment() throws NoAccessRightException {
      Note note = makeMe.aNote().byUser(userModel).please();
      controller.createComment(note);
      List<Comment> comments = makeMe.modelFactoryService.commentRepository.findAllByNote(note);
      assertThat(comments, hasSize(1));
    }

    @Test
    void shouldNotBeAbleToAddCommentToNoteTheUserCannotSee() {
      User anotherUser = makeMe.aUser().please();
      Note note = makeMe.aNote().byUser(anotherUser).please();
      assertThrows(NoAccessRightException.class, () -> controller.createComment(note));
    }

  }
}

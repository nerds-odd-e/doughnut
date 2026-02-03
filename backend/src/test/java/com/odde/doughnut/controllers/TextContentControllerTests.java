package com.odde.doughnut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.odde.doughnut.controllers.dto.NoteRealm;
import com.odde.doughnut.controllers.dto.NoteUpdateDetailsDTO;
import com.odde.doughnut.controllers.dto.NoteUpdateTitleDTO;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.exceptions.UnexpectedNoAccessRightException;
import java.io.IOException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class TextContentControllerTests extends ControllerTestBase {
  @Autowired TextContentController controller;
  Note note;

  @BeforeEach
  void setup() {
    currentUser.setUser(makeMe.aUser().please());
    note = makeMe.aNote("new").creatorAndOwner(currentUser.getUser()).please();
  }

  @Nested
  class updateNoteTopticTest {
    NoteUpdateTitleDTO noteUpdateTitleDTO = new NoteUpdateTitleDTO();

    @BeforeEach
    void setup() {
      noteUpdateTitleDTO.setNewTitle("new title");
    }

    @Test
    void shouldBeAbleToSaveNoteTitle() throws UnexpectedNoAccessRightException {
      NoteRealm response = controller.updateNoteTitle(note, noteUpdateTitleDTO);
      assertThat(response.getId(), equalTo(note.getId()));
      assertThat(response.getNote().getTitle(), equalTo("new title"));
    }

    @Test
    void shouldPreserveRecallSettingsWhenUpdatingTitle() throws UnexpectedNoAccessRightException {
      // Set remember spelling to true
      note.getRecallSetting().setRememberSpelling(true);
      makeMe.refresh(note);

      // Update the title
      NoteRealm response = controller.updateNoteTitle(note, noteUpdateTitleDTO);

      // Verify recall settings are preserved
      makeMe.refresh(note);
      assertThat(note.getRecallSetting().getRememberSpelling(), equalTo(true));
    }

    @Test
    void shouldNotAllowOthersToChange() {
      note = makeMe.aNote("another").creatorAndOwner(makeMe.aUser().please()).please();
      assertThrows(
          UnexpectedNoAccessRightException.class,
          () -> controller.updateNoteTitle(note, noteUpdateTitleDTO));
    }
  }

  @Nested
  class updateNoteDetailsTest {
    NoteUpdateDetailsDTO noteUpdateDetailsDTO = new NoteUpdateDetailsDTO();

    @BeforeEach
    void setup() {
      noteUpdateDetailsDTO.setDetails("new details");
    }

    @Test
    void shouldBeAbleToSaveNoteWhenValid() throws UnexpectedNoAccessRightException, IOException {
      NoteRealm response = controller.updateNoteDetails(note, noteUpdateDetailsDTO);
      assertThat(response.getId(), equalTo(note.getId()));
      assertThat(response.getNote().getDetails(), equalTo("new details"));
    }
  }
}

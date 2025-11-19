package com.odde.doughnut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.odde.doughnut.controllers.currentUser.CurrentUser;
import com.odde.doughnut.controllers.dto.NoteRealm;
import com.odde.doughnut.controllers.dto.NoteUpdateDetailsDTO;
import com.odde.doughnut.controllers.dto.NoteUpdateTitleDTO;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.exceptions.UnexpectedNoAccessRightException;
import com.odde.doughnut.factoryServices.ModelFactoryService;
import com.odde.doughnut.services.AuthorizationService;
import com.odde.doughnut.testability.MakeMe;
import com.odde.doughnut.testability.TestabilitySettings;
import java.io.IOException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class TextContentControllerTests {
  @Autowired ModelFactoryService modelFactoryService;
  @Autowired AuthorizationService authorizationService;

  @Autowired MakeMe makeMe;
  private CurrentUser userModel;
  TextContentController controller;
  private final TestabilitySettings testabilitySettings = new TestabilitySettings();
  Note note;

  @BeforeEach
  void setup() {
    userModel = new CurrentUser(makeMe.aUser().toModelPlease());
    note =
        makeMe
            .aNote("new")
            .creatorAndOwner(makeMe.modelFactoryService.toUserModel(userModel.getUser()))
            .please();
    controller =
        new TextContentController(
            modelFactoryService, userModel, testabilitySettings, authorizationService);
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
      assertThat(response.getNote().getTopicConstructor(), equalTo("new title"));
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

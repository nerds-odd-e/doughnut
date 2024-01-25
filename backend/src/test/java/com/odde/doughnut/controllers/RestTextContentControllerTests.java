package com.odde.doughnut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.odde.doughnut.controllers.json.NoteRealm;
import com.odde.doughnut.controllers.json.NoteUpdateDetailsDTO;
import com.odde.doughnut.controllers.json.NoteUpdateTopicDTO;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.exceptions.UnexpectedNoAccessRightException;
import com.odde.doughnut.factoryServices.ModelFactoryService;
import com.odde.doughnut.models.UserModel;
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
class RestTextContentControllerTests {
  @Autowired ModelFactoryService modelFactoryService;

  @Autowired MakeMe makeMe;
  private UserModel userModel;
  RestTextContentController controller;
  private final TestabilitySettings testabilitySettings = new TestabilitySettings();
  Note note;

  @BeforeEach
  void setup() {
    userModel = makeMe.aUser().toModelPlease();
    note = makeMe.aNote("new").creatorAndOwner(userModel).please();
    controller = new RestTextContentController(modelFactoryService, userModel, testabilitySettings);
  }

  @Nested
  class updateNoteTopticTest {
    NoteUpdateTopicDTO noteUpdateTopicDTO = new NoteUpdateTopicDTO();

    @BeforeEach
    void setup() {
      noteUpdateTopicDTO.setTopicConstructor("new title");
    }

    @Test
    void shouldBeAbleToSaveNoteTopic() throws UnexpectedNoAccessRightException, IOException {
      NoteRealm response = controller.updateNoteTopicConstructor(note, noteUpdateTopicDTO);
      assertThat(response.getId(), equalTo(note.getId()));
      assertThat(response.getNote().getTopicConstructor(), equalTo("new title"));
    }

    @Test
    void shouldNotAllowOthersToChange() {
      note = makeMe.aNote("another").creatorAndOwner(makeMe.aUser().please()).please();
      assertThrows(
          UnexpectedNoAccessRightException.class,
          () -> controller.updateNoteTopicConstructor(note, noteUpdateTopicDTO));
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

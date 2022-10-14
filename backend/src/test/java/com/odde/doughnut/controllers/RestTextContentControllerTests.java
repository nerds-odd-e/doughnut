package com.odde.doughnut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.TextContent;
import com.odde.doughnut.entities.json.NoteRealm;
import com.odde.doughnut.exceptions.UnexpectedNoAccessRightException;
import com.odde.doughnut.factoryServices.ModelFactoryService;
import com.odde.doughnut.models.UserModel;
import com.odde.doughnut.testability.MakeMe;
import com.odde.doughnut.testability.TestabilitySettings;
import java.io.IOException;
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
class RestTextContentControllerTests {
  @Autowired ModelFactoryService modelFactoryService;

  @Autowired MakeMe makeMe;
  private UserModel userModel;
  RestTextContentController controller;
  private final TestabilitySettings testabilitySettings = new TestabilitySettings();

  @BeforeEach
  void setup() {
    userModel = makeMe.aUser().toModelPlease();
    controller = new RestTextContentController(modelFactoryService, userModel, testabilitySettings);
  }

  @Nested
  class updateNoteTest {
    Note note;
    TextContent textContent = new TextContent();

    @BeforeEach
    void setup() {
      note = makeMe.aNote("new").creatorAndOwner(userModel).please();
      textContent.setTitle("new title");
      textContent.setDescription("new description");
    }

    @Test
    void shouldBeAbleToSaveNoteWhenValid() throws UnexpectedNoAccessRightException, IOException {
      NoteRealm response = controller.updateNote(note, textContent);
      assertThat(response.getId(), equalTo(note.getId()));
      assertThat(response.getNote().getTextContent().getDescription(), equalTo("new description"));
    }

    @Test
    void shouldNotAllowOthersToChange() {
      note = makeMe.aNote("another").creatorAndOwner(makeMe.aUser().please()).please();
      assertThrows(UnexpectedNoAccessRightException.class, () -> controller.updateNote(note, textContent));
    }
  }
}

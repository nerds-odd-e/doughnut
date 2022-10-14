package com.odde.doughnut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.Notebook;
import com.odde.doughnut.entities.User;
import com.odde.doughnut.exceptions.UnexpectedNoAccessRightException;
import com.odde.doughnut.factoryServices.ModelFactoryService;
import com.odde.doughnut.models.UserModel;
import com.odde.doughnut.testability.MakeMe;
import com.odde.doughnut.testability.TestabilitySettings;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = {"classpath:repository.xml"})
@Transactional
class RestNotebookControllerTest {
  @Autowired ModelFactoryService modelFactoryService;

  @Autowired MakeMe makeMe;
  private UserModel userModel;
  private Note topNote;
  RestNotebookController controller;
  private TestabilitySettings testabilitySettings = new TestabilitySettings();

  @BeforeEach
  void setup() {
    userModel = makeMe.aUser().toModelPlease();
    topNote = makeMe.aNote().creatorAndOwner(userModel).please();
    controller = new RestNotebookController(modelFactoryService, userModel, testabilitySettings);
  }

  @Nested
  class showNoteTest {
    @Test
    void whenNotLogin() {
      userModel = modelFactoryService.toUserModel(null);
      controller = new RestNotebookController(modelFactoryService, userModel, testabilitySettings);
      assertThrows(ResponseStatusException.class, () -> controller.myNotebooks());
    }

    @Test
    void whenLoggedIn() {
      User user = new User();
      userModel = modelFactoryService.toUserModel(user);
      List<Notebook> notebooks = userModel.getEntity().getOwnership().getNotebooks();
      controller = new RestNotebookController(modelFactoryService, userModel, testabilitySettings);
      assertEquals(notebooks, controller.myNotebooks().notebooks);
    }
  }

  @Nested
  class ShareMyNotebook {

    @Test
    void shareMyNote() throws UnexpectedNoAccessRightException {
      long oldCount = modelFactoryService.bazaarNotebookRepository.count();
      controller.shareNote(topNote.getNotebook());
      assertThat(modelFactoryService.bazaarNotebookRepository.count(), equalTo(oldCount + 1));
    }

    @Test
    void shouldNotBeAbleToShareNoteThatBelongsToOtherUser() {
      User anotherUser = makeMe.aUser().please();
      Note note = makeMe.aNote().creatorAndOwner(anotherUser).please();
      assertThrows(
          UnexpectedNoAccessRightException.class, () -> controller.shareNote(note.getNotebook()));
    }
  }

  @Nested
  class updateNotebook {
    @Test
    void shouldNotBeAbleToUpdateNotebookThatBelongsToOtherUser() {
      User anotherUser = makeMe.aUser().please();
      Note note = makeMe.aNote().creatorAndOwner(anotherUser).please();
      assertThrows(
          UnexpectedNoAccessRightException.class, () -> controller.update(note.getNotebook()));
    }
  }
}

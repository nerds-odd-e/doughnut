package com.odde.doughnut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;

import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.Notebook;
import com.odde.doughnut.exceptions.UnexpectedNoAccessRightException;
import com.odde.doughnut.factoryServices.ModelFactoryService;
import com.odde.doughnut.models.UserModel;
import com.odde.doughnut.testability.MakeMe;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class RestObsidianImportControllerTests {
  @Autowired ModelFactoryService modelFactoryService;
  @Autowired MakeMe makeMe;
  private UserModel userModel;
  private RestObsidianImportController controller;

  @BeforeEach
  void setup() {
    userModel = makeMe.aUser().toModelPlease();
    controller = new RestObsidianImportController(userModel);
  }

  @Nested
  class ImportObsidianTest {
    private Note note1;
    private Notebook notebook;
    private MockMultipartFile zipFile;

    @BeforeEach
    void setup() {
      // Create notebook with Note1
      notebook = makeMe.aNotebook().creatorAndOwner(userModel).please();
      note1 =
          makeMe
              .aNote("Note 1")
              .under(notebook.getHeadNote())
              .details("Content of Note 1")
              .please();

      // Create mock zip file
      zipFile =
          new MockMultipartFile(
              "file", "obsidian.zip", "application/zip", "# Note2\nContent of Note 2".getBytes());
    }

    //    @Test
    void shouldReturnNote1WhenUserHasAccess() throws UnexpectedNoAccessRightException {
      // Act
      NoteRealm response = controller.importObsidian(zipFile, notebook.getId());

      // Assert
      assertThat(response.getId(), equalTo(note1.getId()));
      assertThat(response.getNote().getTopicConstructor(), equalTo("Note1"));
      assertThat(response.getNote().getDetails(), equalTo("Content of Note 1"));
    }

    @Test
    void shouldNotBeAbleToAccessNotebookIDontHaveAccessTo() {
      // Arrange
      UserModel otherUserModel = makeMe.aUser().toModelPlease();
      Notebook otherNotebook = makeMe.aNotebook().creatorAndOwner(otherUserModel).please();

      // Act & Assert
      assertThrows(
          UnexpectedNoAccessRightException.class,
          () -> controller.importObsidian(zipFile, otherNotebook.getId()));
    }

    @Test
    void shouldThrowExceptionForNonExistentNotebook() {
      // Act & Assert
      assertThrows(
          UnexpectedNoAccessRightException.class, () -> controller.importObsidian(zipFile, 99999));
    }

    @Test
    void shouldRequireUserToBeLoggedIn() {
      // Arrange
      userModel = makeMe.aNullUserModelPlease();
      controller = new RestObsidianImportController(userModel);

      // Act & Assert
      ResponseStatusException exception =
          assertThrows(
              ResponseStatusException.class,
              () -> controller.importObsidian(zipFile, notebook.getId()));

      // Verify the correct status and message
      assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatusCode());
      assertEquals("User Not Found", exception.getReason());
    }
  }
}

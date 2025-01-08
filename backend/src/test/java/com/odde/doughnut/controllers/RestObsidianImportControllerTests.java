package com.odde.doughnut.controllers;

import static org.junit.jupiter.api.Assertions.*;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.Notebook;
import com.odde.doughnut.exceptions.UnexpectedNoAccessRightException;
import com.odde.doughnut.factoryServices.ModelFactoryService;
import com.odde.doughnut.models.UserModel;
import com.odde.doughnut.testability.MakeMe;
import com.odde.doughnut.testability.TestabilitySettings;
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

import java.io.IOException;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class RestObsidianImportControllerTests {
  @Autowired ModelFactoryService modelFactoryService;
  @Autowired MakeMe makeMe;
  private UserModel userModel;
  private RestObsidianImportController controller;

    private final TestabilitySettings testabilitySettings = new TestabilitySettings();
  @BeforeEach
  void setup() {
    userModel = makeMe.aUser().toModelPlease();
    controller = new RestObsidianImportController(modelFactoryService, userModel, testabilitySettings);
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

      // Create mock zip file from actual test resource
      try {
        byte[] zipContent = getClass().getResourceAsStream("/import-one-child.zip").readAllBytes();
        zipFile = new MockMultipartFile(
            "file", "obsidian.zip", "application/zip", zipContent);
      } catch (IOException e) {
        throw new RuntimeException("Failed to read test zip file", e);
      }
    }

    @Test
    void shouldSucceedWhenUserHasAccess() throws UnexpectedNoAccessRightException, IOException {
      // Act & Assert - should not throw exception
      controller.importObsidian(zipFile, notebook);
    }

    @Test
    void shouldNotBeAbleToAccessNotebookIDontHaveAccessTo() {
      // Arrange
      UserModel otherUserModel = makeMe.aUser().toModelPlease();
      Notebook otherNotebook = makeMe.aNotebook().creatorAndOwner(otherUserModel).please();

      // Act & Assert
      assertThrows(
          UnexpectedNoAccessRightException.class,
          () -> controller.importObsidian(zipFile, otherNotebook));
    }

    @Test
    void shouldRequireUserToBeLoggedIn() {
      // Arrange
      userModel = makeMe.aNullUserModelPlease();
      controller = new RestObsidianImportController(modelFactoryService,userModel, testabilitySettings);

      // Act & Assert
      ResponseStatusException exception =
          assertThrows(
              ResponseStatusException.class, () -> controller.importObsidian(zipFile, notebook));

      // Verify the correct status and message
      assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatusCode());
      assertEquals("User Not Found", exception.getReason());
    }
  }
}

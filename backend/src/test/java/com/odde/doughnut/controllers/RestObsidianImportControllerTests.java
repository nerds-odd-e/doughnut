package com.odde.doughnut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.*;

import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.Notebook;
import com.odde.doughnut.exceptions.UnexpectedNoAccessRightException;
import com.odde.doughnut.factoryServices.ModelFactoryService;
import com.odde.doughnut.models.UserModel;
import com.odde.doughnut.services.ObsidianFormatService;
import com.odde.doughnut.testability.MakeMe;
import com.odde.doughnut.testability.TestabilitySettings;
import java.io.IOException;
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
  @Autowired ObsidianFormatService obsidianFormatService;
  private UserModel userModel;
  private RestObsidianImportController controller;

  private final TestabilitySettings testabilitySettings = new TestabilitySettings();

  @BeforeEach
  void setup() {
    userModel = makeMe.aUser().toModelPlease();
    controller =
        new RestObsidianImportController(modelFactoryService, userModel, testabilitySettings);
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
              .aNote("note 1")
              .under(notebook.getHeadNote())
              .details("Content of Note 1")
              .please();

      // Create mock zip file from actual test resource
      try {
        byte[] zipContent = getClass().getResourceAsStream("/import-one-child.zip").readAllBytes();
        zipFile = new MockMultipartFile("file", "obsidian.zip", "application/zip", zipContent);
      } catch (IOException e) {
        throw new RuntimeException("Failed to read test zip file", e);
      }
    }

    @Test
    void shouldPreserveExistingNoteContent() throws UnexpectedNoAccessRightException, IOException {
      // Act
      controller.importObsidian(zipFile, notebook);

      // Assert
      Note existingNote =
          notebook.getHeadNote().getChildren().stream()
              .filter(n -> n.getTopicConstructor().equals("note 1"))
              .findFirst()
              .orElseThrow();

      assertThat(existingNote.getTopicConstructor(), equalTo("note 1"));
      assertThat(existingNote.getDetails(), equalTo("Content of Note 1"));
    }

    @Test
    void shouldImportNewNoteWithCorrectContent()
        throws UnexpectedNoAccessRightException, IOException {
      // Act
      controller.importObsidian(zipFile, notebook);
      makeMe.refresh(note1);

      // Assert
      Note importedNote = note1.getChildren().stream().findFirst().orElseThrow();

      assertThat(importedNote.getTopicConstructor(), equalTo("note 2"));
      assertThat(importedNote.getDetails(), equalTo("note 2"));
    }

    @Test
    void shouldEstablishCorrectHierarchy() throws UnexpectedNoAccessRightException, IOException {
      // Act
      controller.importObsidian(zipFile, notebook);
      makeMe.refresh(note1);

      // Assert
      Note note2 = note1.getChildren().stream().findFirst().orElseThrow();

      assertThat(note2.getParent(), equalTo(note1));
      assertThat(note1.getChildren().size(), equalTo(1));
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
      controller =
          new RestObsidianImportController(modelFactoryService, userModel, testabilitySettings);

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

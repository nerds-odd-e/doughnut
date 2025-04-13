package com.odde.doughnut.controllers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.Notebook;
import com.odde.doughnut.factoryServices.ModelFactoryService;
import com.odde.doughnut.models.ExportDataSchema;
import com.odde.doughnut.models.UserModel;
import com.odde.doughnut.testability.MakeMe;
import com.odde.doughnut.testability.TestabilitySettings;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.server.ResponseStatusException;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ExportControllerTest {
  @Autowired ModelFactoryService modelFactoryService;
  @Autowired MakeMe makeMe;
  private UserModel userModel;
  private Notebook notebook;
  private Note headNote;
  private Note tailNote;

  ExportController controller;
  private TestabilitySettings testabilitySettings = new TestabilitySettings();
  @Autowired WebApplicationContext webApplicationContext;

  @BeforeEach
  void setup() {
    MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
    userModel = makeMe.aUser().toModelPlease();
    notebook = makeMe.aNotebook().owner(userModel.getEntity()).please();
    headNote =
        makeMe
            .aNote()
            .creatorAndOwner(userModel.getEntity())
            .under(notebook.getHeadNote())
            .please();
    tailNote =
        makeMe
            .aNote()
            .creatorAndOwner(userModel.getEntity())
            .under(notebook.getHeadNote())
            .please();
    controller = new ExportController(modelFactoryService, userModel, testabilitySettings);
  }

  @Nested
  class ExportNotebook {
    @Test
    void shouldExportNotebookSuccessfully() throws Exception {
      ResponseEntity<ExportDataSchema> actual = controller.exportNotebook(notebook);
      ExportDataSchema body = actual.getBody();
      assertNotNull(body);

      ExportDataSchema.NotebookExport exportedNotebook = body.getNotebooks().get(0);
      assertEquals(notebook.getId(), exportedNotebook.getId());
      assertEquals(notebook.getTitle(), exportedNotebook.getTitle());
      assertEquals(userModel.getName(), body.getMetadata().getExportedBy());
      assertTrue(
          ChronoUnit.SECONDS.between(body.getMetadata().getExportedAt(), LocalDateTime.now()) < 2);
      assertEquals(1, body.getNotebooks().size());

      assertEquals(2, exportedNotebook.getNotes().size());
      assertEquals(headNote.getId(), exportedNotebook.getNotes().get(0).getId());
      assertEquals(headNote.getTopicConstructor(), exportedNotebook.getNotes().get(0).getTitle());
      assertEquals(tailNote.getId(), exportedNotebook.getNotes().get(1).getId());
      assertEquals(tailNote.getTopicConstructor(), exportedNotebook.getNotes().get(1).getTitle());
    }

    @Test
    void shouldReturn401WhenUserNotAuthenticated() throws Exception {
      userModel = makeMe.aNullUserModelPlease();
      controller = new ExportController(modelFactoryService, userModel, testabilitySettings);
      ResponseStatusException exception =
          assertThrows(ResponseStatusException.class, () -> controller.exportNotebook(notebook));
      assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatusCode());
      assertEquals("User Not Found", exception.getReason());
    }
  }

  @Nested
  class ExportAllNotebooks {

    @Test
    void shouldExportAllNotebooksSuccessfully() throws Exception {
      ResponseEntity<ExportDataSchema> actual = controller.exportAllNotebooks();
      ExportDataSchema body = actual.getBody();
      assertNotNull(body);
      assertEquals(1, body.getNotebooks().size());

      ExportDataSchema.NotebookExport exportedNotebook = body.getNotebooks().get(0);
      assertEquals(notebook.getId(), exportedNotebook.getId());
      assertEquals(notebook.getTitle(), exportedNotebook.getTitle());
      assertEquals(userModel.getName(), body.getMetadata().getExportedBy());
      assertTrue(
          ChronoUnit.SECONDS.between(body.getMetadata().getExportedAt(), LocalDateTime.now()) < 2);
    }

    @Test
    void shouldReturn401WhenUserNotAuthenticated() throws Exception {
      userModel = makeMe.aNullUserModelPlease();
      controller = new ExportController(modelFactoryService, userModel, testabilitySettings);

      ResponseStatusException exception =
          assertThrows(ResponseStatusException.class, () -> controller.exportAllNotebooks());
      assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatusCode());
      assertEquals("User Not Found", exception.getReason());
    }
  }
}

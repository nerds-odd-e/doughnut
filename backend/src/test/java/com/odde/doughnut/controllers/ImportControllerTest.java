package com.odde.doughnut.controllers;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.odde.doughnut.factoryServices.ModelFactoryService;
import com.odde.doughnut.models.ImportDataSchema;
import com.odde.doughnut.models.ImportResult;
import com.odde.doughnut.models.UserModel;
import com.odde.doughnut.testability.MakeMe;
import com.odde.doughnut.testability.TestabilitySettings;
import java.time.LocalDateTime;
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
class ImportControllerTest {
  @Autowired ModelFactoryService modelFactoryService;
  @Autowired MakeMe makeMe;
  private UserModel userModel;
  private ImportController controller;
  private TestabilitySettings testabilitySettings = new TestabilitySettings();
  @Autowired WebApplicationContext webApplicationContext;

  @BeforeEach
  void setup() {
    MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
    userModel = makeMe.aUser().toModelPlease();
    controller = new ImportController(modelFactoryService, userModel, testabilitySettings);
  }

  @Nested
  class ImportNotebookTest {
    @Test
    void shouldImportValidNotebookSuccessfully() {
      // 有効なインポートデータを作成
      ImportDataSchema importData = createValidImportData();

      // インポートを実行
      ResponseEntity<ImportResult> response = controller.importNotebook(importData);

      // 結果を検証
      assertEquals(HttpStatus.OK, response.getStatusCode());
      ImportResult result = response.getBody();
      assertNotNull(result);
      assertEquals(1, result.getTotalImported());
      assertTrue(result.getWarnings().isEmpty());

      // インポートされたノートブックを検証
      var importedNotebook = result.getNotebooks().get(0);
      assertNotNull(importedNotebook.getNewId());
      assertEquals("Test Notebook", importedNotebook.getTitle());
      assertEquals(1, importedNotebook.getNotesCount());
    }

    @Test
    void shouldHandleDuplicateNotebookTitle() {
      // 既存のノートブックを作成
      makeMe.aNotebook().owner(userModel.getEntity()).please().setTitle("Test Notebook");

      // 同じタイトルのノートブックをインポート
      ImportDataSchema importData = createValidImportData();
      ResponseEntity<ImportResult> response = controller.importNotebook(importData);

      // 結果を検証
      ImportResult result = response.getBody();
      assertNotNull(result);
      assertEquals(1, result.getTotalImported());
      assertEquals(1, result.getWarnings().size());
      assertTrue(result.getWarnings().get(0).contains("renamed"));

      // リネームされたことを確認
      var importedNotebook = result.getNotebooks().get(0);
      assertTrue(importedNotebook.getTitle().contains("(Imported)"));
    }

    @Test
    void shouldRejectUnauthorizedUser() {
      userModel = makeMe.aNullUserModelPlease();
      controller = new ImportController(modelFactoryService, userModel, testabilitySettings);

      ImportDataSchema importData = createValidImportData();

      ResponseStatusException exception =
          assertThrows(ResponseStatusException.class, () -> controller.importNotebook(importData));
      assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatusCode());
    }

    private ImportDataSchema createValidImportData() {
      ImportDataSchema importData = new ImportDataSchema();

      // メタデータを設定
      ImportDataSchema.Metadata metadata = new ImportDataSchema.Metadata();
      metadata.setVersion("1.0.0");
      metadata.setExportedBy("Test User");
      metadata.setExportedAt(LocalDateTime.now());
      importData.setMetadata(metadata);

      // ノートブックデータを設定
      ImportDataSchema.NotebookImport notebook = new ImportDataSchema.NotebookImport();
      notebook.setTitle("Test Notebook");

      // ノートデータを設定
      ImportDataSchema.NoteImport note = new ImportDataSchema.NoteImport();
      note.setTitle("Test Note");
      note.setDetails("Test Details");
      notebook.setNotes(java.util.List.of(note));

      importData.setNotebooks(java.util.List.of(notebook));

      return importData;
    }
  }

  @Nested
  class ImportAllNotebooksTest {
    @Test
    void shouldImportMultipleNotebooksSuccessfully() {
      ImportDataSchema importData = createMultipleNotebooksImportData();

      ResponseEntity<ImportResult> response = controller.importAllNotebooks(importData);

      ImportResult result = response.getBody();
      assertNotNull(result);
      assertEquals(2, result.getTotalImported());
      assertTrue(result.getWarnings().isEmpty());
    }

    private ImportDataSchema createMultipleNotebooksImportData() {
      ImportDataSchema importData = new ImportDataSchema();

      // メタデータを設定
      ImportDataSchema.Metadata metadata = new ImportDataSchema.Metadata();
      metadata.setVersion("1.0.0");
      metadata.setExportedBy("Test User");
      metadata.setExportedAt(LocalDateTime.now());
      importData.setMetadata(metadata);

      // 複数のノートブックを作成
      ImportDataSchema.NotebookImport notebook1 = new ImportDataSchema.NotebookImport();
      notebook1.setTitle("Notebook 1");
      notebook1.setNotes(java.util.List.of(createTestNote("Note 1", "Details 1")));

      ImportDataSchema.NotebookImport notebook2 = new ImportDataSchema.NotebookImport();
      notebook2.setTitle("Notebook 2");
      notebook2.setNotes(java.util.List.of(createTestNote("Note 2", "Details 2")));

      importData.setNotebooks(java.util.List.of(notebook1, notebook2));

      return importData;
    }

    private ImportDataSchema.NoteImport createTestNote(String title, String details) {
      ImportDataSchema.NoteImport note = new ImportDataSchema.NoteImport();
      note.setTitle(title);
      note.setDetails(details);
      return note;
    }
  }
}

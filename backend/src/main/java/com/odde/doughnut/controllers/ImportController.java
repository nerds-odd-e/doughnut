package com.odde.doughnut.controllers;

import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.Notebook;
import com.odde.doughnut.factoryServices.ModelFactoryService;
import com.odde.doughnut.models.ImportDataSchema;
import com.odde.doughnut.models.ImportResult;
import com.odde.doughnut.models.UserModel;
import com.odde.doughnut.testability.TestabilitySettings;
import jakarta.annotation.Resource;
import java.sql.Timestamp;
import java.util.Objects;
import java.util.stream.StreamSupport;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.annotation.SessionScope;
import org.springframework.web.server.ResponseStatusException;

@RestController
@SessionScope
@RequestMapping("/api/import")
public class ImportController {
  private final ModelFactoryService modelFactoryService;
  private final UserModel currentUser;

  @Resource(name = "testabilitySettings")
  private final TestabilitySettings testabilitySettings;

  public ImportController(
      ModelFactoryService modelFactoryService,
      UserModel currentUser,
      TestabilitySettings testabilitySettings) {
    this.modelFactoryService = modelFactoryService;
    this.currentUser = currentUser;
    this.testabilitySettings = testabilitySettings;
  }

  @PostMapping("/notebooks")
  public ResponseEntity<ImportResult> importNotebook(@RequestBody ImportDataSchema importData) {
    validateUser();
    validateImportData(importData);

    ImportResult result = new ImportResult();

    // 最初のノートブックのみをインポート
    if (!importData.getNotebooks().isEmpty()) {
      ImportDataSchema.NotebookImport notebookData = importData.getNotebooks().get(0);
      importNotebook(notebookData, result);
    }

    return ResponseEntity.ok(result);
  }

  @PostMapping("/all")
  public ResponseEntity<ImportResult> importAllNotebooks(@RequestBody ImportDataSchema importData) {
    validateUser();
    validateImportData(importData);

    ImportResult result = new ImportResult();

    // すべてのノートブックをインポート
    importData.getNotebooks().forEach(notebookData -> importNotebook(notebookData, result));

    return ResponseEntity.ok(result);
  }

  private void validateUser() {
    if (currentUser.getEntity() == null) {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User Not Found");
    }
  }

  private void validateImportData(ImportDataSchema importData) {
    if (importData.getMetadata() == null
        || !Objects.equals(importData.getMetadata().getVersion(), "1.0.0")) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid version");
    }
  }

  private void importNotebook(ImportDataSchema.NotebookImport notebookData, ImportResult result) {
    try {
      // タイトルの重複をチェック
      final String originalTitle = notebookData.getTitle();
      String title = originalTitle;
      boolean isDuplicate =
          StreamSupport.stream(
                  modelFactoryService.notebookRepository.findAll().spliterator(), false)
              .filter(notebook -> currentUser.getEntity().owns(notebook))
              .anyMatch(notebook -> Objects.equals(notebook.getTitle(), originalTitle));

      if (isDuplicate) {
        title = originalTitle + " (Imported)";
        result.addWarning("Notebook '" + originalTitle + "' has been renamed to '" + title + "'");
      }

      // ヘッドノートを作成
      Note headNote = new Note();
      headNote.initialize(
          currentUser.getEntity(), null, new Timestamp(System.currentTimeMillis()), title);

      // ノートブックを作成し、ヘッドノートと関連付け
      headNote.buildNotebookForHeadNote(
          currentUser.getEntity().getOwnership(), currentUser.getEntity());
      Notebook notebook = headNote.getNotebook();

      // ヘッドノートとノートブックを保存
      modelFactoryService.noteRepository.save(headNote);
      modelFactoryService.notebookRepository.save(notebook);

      // 追加のノートをインポート
      for (ImportDataSchema.NoteImport noteData : notebookData.getNotes()) {
        Note note = new Note();
        note.initialize(
            currentUser.getEntity(),
            headNote,
            new Timestamp(System.currentTimeMillis()),
            noteData.getTitle());
        note.setDetails(noteData.getDetails());
        modelFactoryService.noteRepository.save(note);
      }

      // 結果を記録
      ImportResult.NotebookImportResult notebookResult = new ImportResult.NotebookImportResult();
      notebookResult.setNewId(notebook.getId().longValue());
      notebookResult.setTitle(title);
      notebookResult.setNotesCount(notebookData.getNotes().size());
      result.addNotebook(notebookResult);
    } catch (Exception e) {
      // エラーログを出力
      System.err.println("Error importing notebook: " + e.getMessage());
      e.printStackTrace();
      throw e;
    }
  }
}

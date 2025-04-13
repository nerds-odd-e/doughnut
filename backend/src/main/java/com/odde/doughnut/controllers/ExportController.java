package com.odde.doughnut.controllers;

import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.Notebook;
import com.odde.doughnut.factoryServices.ModelFactoryService;
import com.odde.doughnut.models.ExportDataSchema;
import com.odde.doughnut.models.UserModel;
import com.odde.doughnut.testability.TestabilitySettings;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.annotation.Resource;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.StreamSupport;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.annotation.SessionScope;
import org.springframework.web.server.ResponseStatusException;

@RestController
@SessionScope
@RequestMapping("/api/export")
public class ExportController {
  private final ModelFactoryService modelFactoryService;
  private final UserModel currentUser;

  @Resource(name = "testabilitySettings")
  private final TestabilitySettings testabilitySettings;

  public ExportController(
      ModelFactoryService modelFactoryService,
      UserModel currentUser,
      TestabilitySettings testabilitySettings) {
    this.modelFactoryService = modelFactoryService;
    this.currentUser = currentUser;
    this.testabilitySettings = testabilitySettings;
  }

  private ExportDataSchema.NotebookExport toNotebookExport(Notebook notebook) {
    ExportDataSchema.NotebookExport export = new ExportDataSchema.NotebookExport();
    export.setId(notebook.getId());
    export.setTitle(notebook.getTitle());
    List<Note> sortedNotes =
        notebook.getNotes().stream().sorted((a, b) -> Long.compare(a.getId(), b.getId())).toList();
    export.setNotes(sortedNotes.stream().map(this::toNoteExport).toList());
    return export;
  }

  private ExportDataSchema.NoteExport toNoteExport(Note note) {
    ExportDataSchema.NoteExport export = new ExportDataSchema.NoteExport();
    export.setId(note.getId());
    export.setTitle(note.getTopicConstructor());
    export.setDetails(note.getDetails());
    return export;
  }

  private ExportDataSchema createExportDataSchema(List<ExportDataSchema.NotebookExport> notebooks) {
    ExportDataSchema exportDataSchema = new ExportDataSchema();
    exportDataSchema.setNotebooks(notebooks);
    ExportDataSchema.Metadata metadata = new ExportDataSchema.Metadata();
    metadata.setVersion("1.0.0");
    metadata.setExportedBy(currentUser.getName());
    metadata.setExportedAt(LocalDateTime.now());
    exportDataSchema.setMetadata(metadata);
    return exportDataSchema;
  }

  @GetMapping("/notebooks/{notebook}")
  public ResponseEntity<ExportDataSchema> exportNotebook(
      @PathVariable @Schema(type = "integer") Notebook notebook) {
    if (currentUser.getEntity() == null) {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User Not Found");
    }
    if (!currentUser.getEntity().owns(notebook)) {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User Not Found");
    }
    return ResponseEntity.ok(createExportDataSchema(List.of(toNotebookExport(notebook))));
  }

  @GetMapping("/all")
  public ResponseEntity<ExportDataSchema> exportAllNotebooks() {
    if (currentUser.getEntity() == null) {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User Not Found");
    }
    List<Notebook> notebooks =
        StreamSupport.stream(modelFactoryService.notebookRepository.findAll().spliterator(), false)
            .filter(notebook -> currentUser.getEntity().owns(notebook))
            .toList();
    return ResponseEntity.ok(
        createExportDataSchema(notebooks.stream().map(this::toNotebookExport).toList()));
  }
}

package com.odde.doughnut.controllers;

import com.odde.doughnut.controllers.dto.NoteRealm;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.Notebook;
import com.odde.doughnut.exceptions.UnexpectedNoAccessRightException;
import com.odde.doughnut.models.NoteViewer;
import com.odde.doughnut.models.UserModel;
import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;

@RestController
@RequestMapping("/api")
public class RestObsidianImportController {
  private final UserModel currentUser;

  public RestObsidianImportController(UserModel currentUser) {
    this.currentUser = currentUser;
  }

  @Operation(summary = "Import Obsidian file")
  @PostMapping(value = "/obsidian/{parentNoteId}/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public NoteRealm importObsidian(
      @Parameter(description = "Obsidian zip file to import")
      @RequestParam("file") MultipartFile file,
      
      @Parameter(description = "Parent note ID")
      @PathVariable("parentNoteId") @Schema(type = "integer") Integer parentNoteId)
      throws UnexpectedNoAccessRightException {
    currentUser.assertLoggedIn();

    Notebook notebook =
        currentUser.getEntity().getOwnership().getNotebooks().stream()
            .filter(n -> n.getId().equals(parentNoteId))
            .findFirst()
            .orElseThrow(() -> new UnexpectedNoAccessRightException());

    // TODO: Process zip file content
    // For now, just return the head note
    Note note = notebook.getHeadNote();
    return new NoteViewer(currentUser.getEntity(), note).toJsonObject();
  }
}

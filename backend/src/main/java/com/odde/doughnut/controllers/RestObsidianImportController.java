package com.odde.doughnut.controllers;

import com.odde.doughnut.controllers.dto.NoteCreationDTO;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.Notebook;
import com.odde.doughnut.exceptions.UnexpectedNoAccessRightException;
import com.odde.doughnut.factoryServices.ModelFactoryService;
import com.odde.doughnut.models.UserModel;
import com.odde.doughnut.services.NoteConstructionService;
import com.odde.doughnut.testability.TestabilitySettings;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.annotation.SessionScope;
import org.springframework.web.multipart.MultipartFile;

@RestController
@SessionScope
@RequestMapping("/api")
public class RestObsidianImportController {
  private final UserModel currentUser;
  private final NoteConstructionService noteConstructionService;

  public RestObsidianImportController(
      ModelFactoryService modelFactoryService,
      UserModel currentUser,
      TestabilitySettings testabilitySettings) {
    this.currentUser = currentUser;
    this.noteConstructionService =
        new NoteConstructionService(
            currentUser.getEntity(),
            testabilitySettings.getCurrentUTCTimestamp(),
            modelFactoryService);
  }

  @Operation(summary = "Import Obsidian file")
  @PostMapping(
      value = "/obsidian/{notebookId}/import",
      consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @ResponseStatus(HttpStatus.OK)
  public void importObsidian(
      @Parameter(description = "Obsidian zip file to import") @RequestParam("file")
          MultipartFile file,
      @Parameter(description = "Notebook ID") @PathVariable("notebookId") @Schema(type = "integer")
          Notebook notebook)
      throws UnexpectedNoAccessRightException, IOException {
    currentUser.assertLoggedIn();
    currentUser.assertReadAuthorization(notebook);

    try (ZipInputStream zipIn = new ZipInputStream(file.getInputStream())) {
      ZipEntry entry;

      // Find Note1 in the notebook
      Note note1 =
          notebook.getHeadNote().getChildren().stream()
              .filter(note -> note.getTopicConstructor().equals("Note 1"))
              .findFirst()
              .orElseThrow(() -> new UnexpectedNoAccessRightException());

      while ((entry = zipIn.getNextEntry()) != null) {
        String entryName = entry.getName();

        // Skip hidden files/directories (starting with .)
        if (entryName.startsWith(".") || entryName.contains("/.")) {
          continue;
        }

        //        System.out.println("Importing " + entryName);

        // Process directory or file
        String[] pathParts = entryName.split("/");
        Note currentParent = note1;

        // Create notes for each directory in the path
        for (int i = 1; i < pathParts.length; i++) {
          String part = pathParts[i];
          System.out.println("Importing " + part);

          // Skip empty parts and .md extension
          if (part.isEmpty() || part.equals(".md")) {
            continue;
          }

          // Remove .md extension if it's a file
          if (part.endsWith(".md")) {
            part = part.substring(0, part.length() - 3);
          }

          // Check if note already exists under current parent
          String finalPart = part;
          Note existingNote =
              currentParent.getChildren().stream()
                  .filter(note -> note.getNoteTitle().matches(finalPart))
                  .findFirst()
                  .orElse(null);

          if (existingNote == null) {
            // Create new note
            NoteCreationDTO noteCreation = new NoteCreationDTO();
            noteCreation.setNewTitle(part);

            // If it's a file (last part), add the content
            //                      if (!entry.isDirectory() && i == pathParts.length - 1) {
            //                        noteCreation.setDetails(new String(zipIn.readAllBytes()));
            //                      }

            //                      Note newNote = noteConstructionService.createNote(
            //                          currentParent,
            //                          noteCreation,
            //                          currentUser.getEntity(),
            //                          null);
            //                      currentParent = newNote;
          } else {
            currentParent = existingNote;
          }
        }
      }
    }
  }
}

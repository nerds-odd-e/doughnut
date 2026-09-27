package com.odde.donut.controllers;

import com.odde.donut.entities.Note;
import com.odde.donut.exceptions.UnexpectedNoAccessRightException;
import com.odde.donut.services.AuthorizationService;
import com.odde.donut.services.notebookAttachment.NotebookAttachmentFile;
import com.odde.donut.services.notebookGit.NoteFolderAttachment;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.annotation.SessionScope;
import org.springframework.web.server.ResponseStatusException;

@RestController
@SessionScope
@RequestMapping("/api/notes")
class NoteImageController {
  private final AuthorizationService authorizationService;
  private final NoteFolderAttachment noteFolderAttachment;
  private final NotebookAttachmentFile notebookAttachmentFile;

  NoteImageController(
      AuthorizationService authorizationService,
      NoteFolderAttachment noteFolderAttachment,
      NotebookAttachmentFile notebookAttachmentFile) {
    this.authorizationService = authorizationService;
    this.noteFolderAttachment = noteFolderAttachment;
    this.notebookAttachmentFile = notebookAttachmentFile;
  }

  @Operation(
      summary = "Show an image file the note refers to",
      description =
          "The notebook file at a path relative to the note's folder, served inline when it is a"
              + " PNG, JPEG, GIF or WebP image.")
  @GetMapping(
      value = "/{note}/image",
      produces = {
        MediaType.IMAGE_PNG_VALUE,
        MediaType.IMAGE_JPEG_VALUE,
        MediaType.IMAGE_GIF_VALUE,
        "image/webp"
      })
  public ResponseEntity<byte[]> showNoteImage(
      @PathVariable("note") @Schema(type = "integer") Note note, @RequestParam("path") String path)
      throws UnexpectedNoAccessRightException {
    authorizationService.assertReadAuthorization(note);
    return noteFolderAttachment
        .at(note, path)
        .map(attachment -> InlineImage.of(attachment, notebookAttachmentFile))
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "File not found."));
  }
}

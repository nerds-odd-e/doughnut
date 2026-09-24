package com.odde.donut.controllers;

import com.odde.donut.controllers.dto.NotebookAttachmentRealm;
import com.odde.donut.entities.Notebook;
import com.odde.donut.entities.NotebookAttachment;
import com.odde.donut.exceptions.UnexpectedNoAccessRightException;
import com.odde.donut.services.AuthorizationService;
import com.odde.donut.services.NotebookCatalogService;
import com.odde.donut.services.notebookAttachment.NotebookAttachmentFile;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import java.nio.charset.StandardCharsets;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.annotation.SessionScope;

@RestController
@SessionScope
@RequestMapping("/api/notebooks")
class NotebookAttachmentController {
  private final AuthorizationService authorizationService;
  private final NotebookCatalogService notebookCatalogService;
  private final NotebookAttachmentFile notebookAttachmentFile;

  NotebookAttachmentController(
      AuthorizationService authorizationService,
      NotebookCatalogService notebookCatalogService,
      NotebookAttachmentFile notebookAttachmentFile) {
    this.authorizationService = authorizationService;
    this.notebookCatalogService = notebookCatalogService;
    this.notebookAttachmentFile = notebookAttachmentFile;
  }

  @Operation(
      summary = "Get file page payload",
      description = "Notebook chrome, folder trail through the file's folder, filename, and size.")
  @GetMapping("/{notebook}/attachments/{attachment}")
  public NotebookAttachmentRealm getAttachmentPage(
      @PathVariable("notebook") @Schema(type = "integer") Notebook notebook,
      @PathVariable("attachment") @Schema(type = "integer") NotebookAttachment attachment)
      throws UnexpectedNoAccessRightException {
    authorizationService.assertReadAuthorization(notebook);
    attachment.requireInNotebook(notebook);
    return NotebookAttachmentRealm.of(
        notebookCatalogService.notebookRealmFor(notebook, authorizationService.getCurrentUser()),
        attachment,
        notebookAttachmentFile.size(attachment));
  }

  @Operation(
      summary = "Download a file's exact bytes",
      description = "Always an attachment download under the file's name; never rendered inline.")
  @GetMapping(
      value = "/{notebook}/attachments/{attachment}/content",
      produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
  public ResponseEntity<byte[]> downloadAttachment(
      @PathVariable("notebook") @Schema(type = "integer") Notebook notebook,
      @PathVariable("attachment") @Schema(type = "integer") NotebookAttachment attachment)
      throws UnexpectedNoAccessRightException {
    authorizationService.assertReadAuthorization(notebook);
    attachment.requireInNotebook(notebook);
    return ResponseEntity.ok()
        .header(
            HttpHeaders.CONTENT_DISPOSITION,
            ContentDisposition.attachment()
                .filename(attachment.getFilename(), StandardCharsets.UTF_8)
                .build()
                .toString())
        .header("X-Content-Type-Options", "nosniff")
        .contentType(MediaType.APPLICATION_OCTET_STREAM)
        .body(notebookAttachmentFile.bytes(attachment));
  }
}

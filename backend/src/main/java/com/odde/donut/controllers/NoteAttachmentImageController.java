package com.odde.donut.controllers;

import com.odde.donut.entities.Note;
import com.odde.donut.exceptions.UnexpectedNoAccessRightException;
import com.odde.donut.services.AuthorizationService;
import com.odde.donut.services.notebookAttachment.NotebookAttachmentFile;
import com.odde.donut.services.notebookGit.NoteFolderAttachment;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
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
class NoteAttachmentImageController {
  private static final Map<String, MediaType> RASTER_TYPES =
      Map.of(
          "png", MediaType.IMAGE_PNG,
          "jpg", MediaType.IMAGE_JPEG,
          "jpeg", MediaType.IMAGE_JPEG,
          "gif", MediaType.IMAGE_GIF,
          "webp", MediaType.parseMediaType("image/webp"));

  private final AuthorizationService authorizationService;
  private final NoteFolderAttachment noteFolderAttachment;
  private final NotebookAttachmentFile notebookAttachmentFile;

  NoteAttachmentImageController(
      AuthorizationService authorizationService,
      NoteFolderAttachment noteFolderAttachment,
      NotebookAttachmentFile notebookAttachmentFile) {
    this.authorizationService = authorizationService;
    this.noteFolderAttachment = noteFolderAttachment;
    this.notebookAttachmentFile = notebookAttachmentFile;
  }

  @Operation(
      summary = "Show a picture file the note refers to",
      description =
          "The notebook file at a path relative to the note's folder, served inline when it is a"
              + " PNG, JPEG, GIF or WebP picture.")
  @GetMapping(
      value = "/{note}/attachment-image",
      produces = {
        MediaType.IMAGE_PNG_VALUE,
        MediaType.IMAGE_JPEG_VALUE,
        MediaType.IMAGE_GIF_VALUE,
        "image/webp"
      })
  public ResponseEntity<byte[]> showAttachmentImage(
      @PathVariable("note") @Schema(type = "integer") Note note, @RequestParam("path") String path)
      throws UnexpectedNoAccessRightException {
    authorizationService.assertReadAuthorization(note);
    MediaType mediaType =
        rasterType(path)
            .orElseThrow(
                () ->
                    new ResponseStatusException(
                        HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Not a raster picture."));
    return noteFolderAttachment
        .at(note, path)
        .map(
            attachment ->
                ResponseEntity.ok()
                    .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.inline().build().toString())
                    .header("X-Content-Type-Options", "nosniff")
                    .contentType(mediaType)
                    .body(notebookAttachmentFile.bytes(attachment)))
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "File not found."));
  }

  private static Optional<MediaType> rasterType(String path) {
    return Optional.ofNullable(StringUtils.getFilenameExtension(path))
        .map(extension -> RASTER_TYPES.get(extension.toLowerCase(Locale.ROOT)));
  }
}

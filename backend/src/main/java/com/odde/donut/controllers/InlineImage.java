package com.odde.donut.controllers;

import com.odde.donut.entities.NotebookAttachment;
import com.odde.donut.services.notebookAttachment.ImageFile;
import com.odde.donut.services.notebookAttachment.NotebookAttachmentFile;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ResponseStatusException;

/** A notebook file shown inline as an image, refused when its name is not an image's. */
final class InlineImage {
  private InlineImage() {}

  static ResponseEntity<byte[]> of(
      NotebookAttachment attachment, NotebookAttachmentFile notebookAttachmentFile) {
    MediaType mediaType =
        ImageFile.mediaType(attachment.getFilename())
            .orElseThrow(
                () ->
                    new ResponseStatusException(
                        HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Not a raster picture."));
    return ResponseEntity.ok()
        .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.inline().build().toString())
        .header("X-Content-Type-Options", "nosniff")
        .contentType(mediaType)
        .body(notebookAttachmentFile.bytes(attachment));
  }
}

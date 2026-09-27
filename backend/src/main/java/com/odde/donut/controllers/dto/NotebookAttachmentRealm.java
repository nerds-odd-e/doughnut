package com.odde.donut.controllers.dto;

import com.fasterxml.jackson.annotation.JsonUnwrapped;
import com.odde.donut.entities.NotebookAttachment;
import com.odde.donut.services.notebookAttachment.PictureFile;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(
    description =
        "Notebook chrome plus one file for loading the file page: the shared realm sidebar with the"
            + " folder trail from notebook root through the file's folder, the file's id and filename, and its"
            + " size in bytes, and whether it is a picture the page can show.")
public record NotebookAttachmentRealm(
    @NotNull @JsonUnwrapped RealmNotebookSidebar sidebar,
    @NotNull NotebookAttachmentListItem attachment,
    @NotNull long size,
    @NotNull boolean picture) {

  public static NotebookAttachmentRealm of(
      NotebookRealm chrome, NotebookAttachment attachment, long size) {
    RealmNotebookSidebar sidebar = new RealmNotebookSidebar();
    sidebar.setNotebookRealm(chrome);
    sidebar.setAncestorFolders(FolderTrailSegments.fromRootToFolder(attachment.getFolder()));
    return new NotebookAttachmentRealm(
        sidebar,
        new NotebookAttachmentListItem(attachment.getId(), attachment.getFilename()),
        size,
        PictureFile.mediaType(attachment.getFilename()).isPresent());
  }
}

package com.odde.donut.controllers.dto;

import com.fasterxml.jackson.annotation.JsonUnwrapped;
import com.odde.donut.entities.NotebookAttachment;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(
    description =
        "Notebook chrome plus one file for loading the file page: the shared realm sidebar with the"
            + " folder trail from notebook root through the file's folder, the file row, and its"
            + " size in bytes.")
public record NotebookAttachmentRealm(
    @NotNull @JsonUnwrapped RealmNotebookSidebar sidebar,
    @NotNull NotebookAttachment attachment,
    @NotNull long size) {

  public static NotebookAttachmentRealm of(
      NotebookRealm chrome, NotebookAttachment attachment, long size) {
    RealmNotebookSidebar sidebar = new RealmNotebookSidebar();
    sidebar.setNotebookRealm(chrome);
    sidebar.setAncestorFolders(FolderTrailSegments.fromRootToFolder(attachment.getFolder()));
    return new NotebookAttachmentRealm(sidebar, attachment, size);
  }
}

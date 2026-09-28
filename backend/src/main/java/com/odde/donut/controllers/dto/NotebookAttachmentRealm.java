package com.odde.donut.controllers.dto;

import com.fasterxml.jackson.annotation.JsonUnwrapped;
import com.odde.donut.entities.Folder;
import com.odde.donut.entities.NotebookAttachment;
import com.odde.donut.services.notebookAttachment.ImageFile;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import java.util.List;

@Schema(
    description =
        "Notebook chrome plus one file for loading the file page: the shared realm sidebar with the"
            + " folder trail from notebook root through the file's folder, the file's id and filename, and its"
            + " size in bytes, and whether it is an image the page can show.")
public record NotebookAttachmentRealm(
    @NotNull @JsonUnwrapped RealmNotebookSidebar sidebar,
    @NotNull NotebookAttachmentListItem attachment,
    @NotNull long size,
    @NotNull boolean image,
    @NotNull List<NoteTopology> references) {

  public static NotebookAttachmentRealm of(
      NotebookRealm chrome,
      NotebookAttachment attachment,
      long size,
      List<NoteTopology> references) {
    RealmNotebookSidebar sidebar = new RealmNotebookSidebar();
    sidebar.setNotebookRealm(chrome);
    sidebar.setAncestorFolders(
        FolderTrailSegment.of(Folder.trailFromRootTo(attachment.getFolder())));
    return new NotebookAttachmentRealm(
        sidebar,
        new NotebookAttachmentListItem(attachment.getId(), attachment.getFilename()),
        size,
        ImageFile.mediaType(attachment.getFilename()).isPresent(),
        references);
  }
}

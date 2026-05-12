package com.odde.doughnut.controllers.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import com.odde.doughnut.entities.Folder;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import java.util.List;

@Schema(
    description =
        "Notebook chrome plus folder row for loading the folder page: same shared realm sidebar as"
            + " NoteRealm (without note-level fields), plus folder identity, optional parent folder id,"
            + " and optional container-owned folder index markdown.")
public record FolderRealm(
    @NotNull @JsonUnwrapped RealmNotebookSidebar sidebar,
    @NotNull Folder folder,
    @JsonInclude(JsonInclude.Include.NON_NULL)
        @Schema(
            description = "Parent folder id when this folder is nested; omitted at notebook root.")
        Integer parentFolderId,
    @JsonInclude(JsonInclude.Include.NON_NULL)
        @Schema(
            description =
                "Container-owned folder index markdown (populated by migration from legacy"
                    + " index note). Omitted when absent.")
        String indexContent) {

  public static FolderRealm of(
      NotebookRealm chrome,
      List<Folder> ancestorFolders,
      String indexNoteContent,
      Folder folder,
      Integer parentFolderId,
      String indexContent) {
    RealmNotebookSidebar sidebar = new RealmNotebookSidebar();
    sidebar.setNotebookRealm(chrome);
    sidebar.setAncestorFolders(ancestorFolders);
    sidebar.setIndexNoteContent(indexNoteContent);
    return new FolderRealm(sidebar, folder, parentFolderId, indexContent);
  }
}

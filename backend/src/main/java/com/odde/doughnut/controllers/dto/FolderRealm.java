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
            + " and optional container-owned folder readme markdown.")
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
                "Container-owned folder readme markdown (populated by migration from legacy"
                    + " container note). Omitted when absent.")
        String readmeContent) {

  public static FolderRealm of(
      NotebookRealm chrome,
      List<Folder> ancestorFolders,
      String scopedReadmeContent,
      Folder folder,
      Integer parentFolderId,
      String readmeContent) {
    RealmNotebookSidebar sidebar = new RealmNotebookSidebar();
    sidebar.setNotebookRealm(chrome);
    sidebar.setAncestorFolders(ancestorFolders);
    sidebar.setScopedReadmeContent(scopedReadmeContent);
    return new FolderRealm(sidebar, folder, parentFolderId, readmeContent);
  }
}

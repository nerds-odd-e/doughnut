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
            + " and optional designated folder index note id.")
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
                "Folder index landing note id from cached folder.index_note_id when valid;"
                    + " otherwise repaired from notes titled \"index\" in this folder"
                    + " (case-insensitive). Omitted when absent.")
        Integer folderIndexNoteId) {

  public static FolderRealm of(
      NotebookClientView chrome,
      List<Folder> ancestorFolders,
      String indexNoteContent,
      Folder folder,
      Integer parentFolderId,
      Integer folderIndexNoteId) {
    RealmNotebookSidebar sidebar = new RealmNotebookSidebar();
    sidebar.setNotebookView(chrome);
    sidebar.setAncestorFolders(ancestorFolders);
    sidebar.setIndexNoteContent(indexNoteContent);
    return new FolderRealm(sidebar, folder, parentFolderId, folderIndexNoteId);
  }
}

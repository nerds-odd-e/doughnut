package com.odde.doughnut.controllers.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.odde.doughnut.entities.Folder;
import com.odde.doughnut.entities.Notebook;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(
    description =
        "Notebook chrome plus folder row for loading the folder page: same notebook fields as"
            + " NotebookRealm (without notebook-level indexNoteId), plus folder identity,"
            + " optional parent folder id, and optional designated folder index note id.")
public record FolderRealm(
    @NotNull Notebook notebook,
    @JsonInclude(JsonInclude.Include.NON_NULL) Boolean hasAttachedBook,
    boolean readonly,
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
      NotebookClientView chrome, Folder folder, Integer parentFolderId, Integer folderIndexNoteId) {
    return new FolderRealm(
        chrome.notebook(),
        chrome.hasAttachedBook(),
        chrome.readonly(),
        folder,
        parentFolderId,
        folderIndexNoteId);
  }
}

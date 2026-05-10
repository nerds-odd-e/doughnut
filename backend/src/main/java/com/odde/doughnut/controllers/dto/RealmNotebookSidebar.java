package com.odde.doughnut.controllers.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.odde.doughnut.entities.Folder;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Schema(
    description =
        "Notebook chrome, ancestor folder trail, and optional index note markdown for scoped title"
            + " patterns (shared by note and folder page realms).")
@Getter
@Setter
public class RealmNotebookSidebar {

  @NotNull
  @Schema(description = "Notebook entity plus optional client-only fields.")
  private NotebookClientView notebookView;

  @Schema(description = "Folders from notebook root outward; see each realm for trail semantics.")
  private List<Folder> ancestorFolders = List.of();

  @JsonInclude(JsonInclude.Include.NON_NULL)
  @Schema(
      description =
          "Full markdown of the designated index note that supplies the nearest non-blank title_pattern"
              + " (inner scope toward notebook root). Omitted when none applies.")
  private String indexNoteContent;
}

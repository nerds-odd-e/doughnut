package com.odde.doughnut.controllers.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.odde.doughnut.entities.Notebook;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(
    description =
        "Notebook entity plus optional client-only fields (e.g. catalog attachment hints).")
public record NotebookClientView(
    @NotNull Notebook notebook,
    @JsonInclude(JsonInclude.Include.NON_NULL) Boolean hasAttachedBook,
    boolean readonly,
    @JsonInclude(JsonInclude.Include.NON_NULL)
        @Schema(
            description =
                "Id of this notebook's index landing note when one exists (root folder scope,"
                    + " title equal to \"index\" case-insensitive); omitted when absent.")
        Integer indexNoteId) {

  public static NotebookClientView of(
      Notebook notebook, Boolean hasAttachedBook, boolean readonly, Integer indexNoteId) {
    return new NotebookClientView(notebook, hasAttachedBook, readonly, indexNoteId);
  }
}

package com.odde.doughnut.controllers.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.odde.doughnut.entities.Notebook;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(
    description =
        "Notebook client view for loading the notebook page: same payload as NotebookClientView"
            + " plus optional index landing note id when present.")
public record NotebookPageClientView(
    @NotNull Notebook notebook,
    @JsonInclude(JsonInclude.Include.NON_NULL) Boolean hasAttachedBook,
    boolean readonly,
    @JsonInclude(JsonInclude.Include.NON_NULL)
        @Schema(
            description =
                "Id of this notebook's index landing note when one exists (root folder scope,"
                    + " title equal to \"index\" case-insensitive); omitted when absent.")
        Integer indexNoteId) {

  public static NotebookPageClientView of(NotebookClientView base, Integer indexNoteId) {
    return new NotebookPageClientView(
        base.notebook(), base.hasAttachedBook(), base.readonly(), indexNoteId);
  }
}

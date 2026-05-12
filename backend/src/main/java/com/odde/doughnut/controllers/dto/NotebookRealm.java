package com.odde.doughnut.controllers.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.odde.doughnut.entities.Notebook;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(
    description =
        "Notebook chrome: entity plus optional catalog hints (attached book, readonly), optional"
            + " notebook index landing note id when present.")
public record NotebookRealm(
    @NotNull Notebook notebook,
    @JsonInclude(JsonInclude.Include.NON_NULL) Boolean hasAttachedBook,
    boolean readonly,
    @JsonInclude(JsonInclude.Include.NON_NULL)
        @Schema(
            description =
                "Notebook index landing note id from cached notebook.index_note_id when valid;"
                    + " otherwise repaired from the sole root note titled \"index\""
                    + " (case-insensitive). Omitted when absent.")
        Integer indexNoteId) {

  public static NotebookRealm of(
      Notebook notebook, Boolean hasAttachedBook, boolean readonly, Integer indexNoteId) {
    return new NotebookRealm(notebook, hasAttachedBook, readonly, indexNoteId);
  }
}

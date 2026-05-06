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
    boolean readonly) {

  public static NotebookClientView of(
      Notebook notebook, Boolean hasAttachedBook, boolean readonly) {
    return new NotebookClientView(notebook, hasAttachedBook, readonly);
  }
}

package com.odde.doughnut.controllers.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import com.odde.doughnut.entities.Notebook;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(
    description =
        "Notebook fields exposed to clients, optionally including attached-book indicator.")
public final class NotebookClientView {

  @JsonUnwrapped private final Notebook notebook;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private final Boolean hasAttachedBook;

  private NotebookClientView(Notebook notebook, Boolean hasAttachedBook) {
    this.notebook = notebook;
    this.hasAttachedBook = hasAttachedBook;
  }

  public static NotebookClientView of(Notebook notebook, boolean hasAttachedBook) {
    return new NotebookClientView(notebook, hasAttachedBook);
  }

  @Schema(hidden = true)
  @JsonIgnore
  public @NotNull Notebook getNotebook() {
    return notebook;
  }

  public Boolean getHasAttachedBook() {
    return hasAttachedBook;
  }
}

package com.odde.doughnut.controllers.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.odde.doughnut.entities.Notebook;
import jakarta.validation.constraints.NotNull;

@JsonPropertyOrder({"type", "notebook", "hasAttachedBook"})
public final class NotebookCatalogNotebookItem implements NotebookCatalogItem {
  @NotNull public Notebook notebook;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  public Boolean hasAttachedBook;

  public NotebookCatalogNotebookItem() {}

  public NotebookCatalogNotebookItem(Notebook notebook, boolean hasAttachedBook) {
    this.notebook = notebook;
    this.hasAttachedBook = hasAttachedBook;
  }
}

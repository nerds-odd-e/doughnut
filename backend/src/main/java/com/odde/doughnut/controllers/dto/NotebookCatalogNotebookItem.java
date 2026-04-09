package com.odde.doughnut.controllers.dto;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.odde.doughnut.entities.Notebook;
import jakarta.validation.constraints.NotNull;

@JsonPropertyOrder({"type", "notebook"})
public final class NotebookCatalogNotebookItem implements NotebookCatalogItem {
  @NotNull public Notebook notebook;

  public NotebookCatalogNotebookItem() {}

  public NotebookCatalogNotebookItem(Notebook notebook) {
    this.notebook = notebook;
  }
}

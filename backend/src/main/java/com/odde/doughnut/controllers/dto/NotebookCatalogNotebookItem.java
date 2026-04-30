package com.odde.doughnut.controllers.dto;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import jakarta.validation.constraints.NotNull;

@JsonPropertyOrder({"type", "notebook"})
public final class NotebookCatalogNotebookItem implements NotebookCatalogItem {
  @NotNull public NotebookClientView notebook;

  public NotebookCatalogNotebookItem() {}

  public NotebookCatalogNotebookItem(NotebookClientView notebook) {
    this.notebook = notebook;
  }
}

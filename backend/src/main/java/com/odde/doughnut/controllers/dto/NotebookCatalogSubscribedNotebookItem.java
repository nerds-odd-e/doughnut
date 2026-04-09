package com.odde.doughnut.controllers.dto;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.odde.doughnut.entities.Notebook;
import jakarta.validation.constraints.NotNull;

@JsonPropertyOrder({"type", "notebook", "subscriptionId"})
public final class NotebookCatalogSubscribedNotebookItem implements NotebookCatalogItem {
  @NotNull public Notebook notebook;
  @NotNull public Integer subscriptionId;

  public NotebookCatalogSubscribedNotebookItem() {}

  public NotebookCatalogSubscribedNotebookItem(Notebook notebook, Integer subscriptionId) {
    this.notebook = notebook;
    this.subscriptionId = subscriptionId;
  }
}

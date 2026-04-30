package com.odde.doughnut.controllers.dto;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import jakarta.validation.constraints.NotNull;

@JsonPropertyOrder({"type", "notebook", "subscriptionId"})
public final class NotebookCatalogSubscribedNotebookItem implements NotebookCatalogItem {
  @NotNull public NotebookClientView notebook;
  @NotNull public Integer subscriptionId;

  public NotebookCatalogSubscribedNotebookItem() {}

  public NotebookCatalogSubscribedNotebookItem(
      NotebookClientView notebook, Integer subscriptionId) {
    this.notebook = notebook;
    this.subscriptionId = subscriptionId;
  }
}

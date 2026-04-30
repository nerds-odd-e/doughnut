package com.odde.doughnut.controllers.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.odde.doughnut.entities.Notebook;
import jakarta.validation.constraints.NotNull;

@JsonPropertyOrder({"type", "notebook", "subscriptionId", "hasAttachedBook"})
public final class NotebookCatalogSubscribedNotebookItem implements NotebookCatalogItem {
  @NotNull public Notebook notebook;
  @NotNull public Integer subscriptionId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  public Boolean hasAttachedBook;

  public NotebookCatalogSubscribedNotebookItem() {}

  public NotebookCatalogSubscribedNotebookItem(
      Notebook notebook, Integer subscriptionId, boolean hasAttachedBook) {
    this.notebook = notebook;
    this.subscriptionId = subscriptionId;
    this.hasAttachedBook = hasAttachedBook;
  }
}

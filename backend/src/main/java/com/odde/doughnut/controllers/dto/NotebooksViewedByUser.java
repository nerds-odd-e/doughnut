package com.odde.doughnut.controllers.dto;

import jakarta.validation.constraints.NotNull;
import java.util.List;

public class NotebooksViewedByUser {
  @NotNull public List<NotebookClientView> notebooks;
  @NotNull public List<NotebookCatalogItem> catalogItems;
  public List<SubscriptionForNotebooksListing> subscriptions;
}

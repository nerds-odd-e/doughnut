package com.odde.doughnut.controllers.dto;

import com.odde.doughnut.entities.Notebook;
import com.odde.doughnut.entities.Subscription;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public class NotebooksViewedByUser {
  @NotNull public List<Notebook> notebooks;
  @NotNull public List<NotebookCatalogItem> catalogItems;
  public List<Subscription> subscriptions;
}

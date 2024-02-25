package com.odde.doughnut.controllers.json;

import com.odde.doughnut.entities.Subscription;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public class NotebooksViewedByUser {
  @NotNull public List<NotebookViewedByUser> notebooks;
  public List<Subscription> subscriptions;
}

package com.odde.doughnut.controllers.dto;

import com.odde.doughnut.entities.Subscription;
import com.odde.doughnut.entities.User;
import jakarta.validation.constraints.NotNull;

public final class SubscriptionForNotebooksListing {

  public @NotNull Integer id;

  public @NotNull Integer dailyTargetOfNewNotes;

  public User user;

  public @NotNull NotebookClientView notebook;

  public static SubscriptionForNotebooksListing from(
      Subscription subscription, NotebookClientView notebookView) {
    SubscriptionForNotebooksListing dto = new SubscriptionForNotebooksListing();
    dto.id = subscription.getId();
    dto.dailyTargetOfNewNotes = subscription.getDailyTargetOfNewNotes();
    dto.user = subscription.getUser();
    dto.notebook = notebookView;
    return dto;
  }

  public String getName() {
    return notebook.getNotebook().getName();
  }
}

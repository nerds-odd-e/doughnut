package com.odde.doughnut.controllers.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.odde.doughnut.entities.Notebook;
import com.odde.doughnut.entities.Subscription;
import com.odde.doughnut.entities.User;
import jakarta.validation.constraints.NotNull;

public final class SubscriptionForNotebooksListing {

  public @NotNull Integer id;

  public @NotNull Integer dailyTargetOfNewNotes;

  public User user;

  public @NotNull Notebook notebook;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  public Boolean hasAttachedBook;

  public static SubscriptionForNotebooksListing from(
      Subscription subscription, Notebook notebook, boolean hasAttachedBook) {
    SubscriptionForNotebooksListing dto = new SubscriptionForNotebooksListing();
    dto.id = subscription.getId();
    dto.dailyTargetOfNewNotes = subscription.getDailyTargetOfNewNotes();
    dto.user = subscription.getUser();
    dto.notebook = notebook;
    dto.hasAttachedBook = hasAttachedBook;
    return dto;
  }

  public String getName() {
    return notebook.getName();
  }
}

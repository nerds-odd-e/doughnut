package com.odde.doughnut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertFalse;

import com.odde.doughnut.controllers.dto.NotebookCatalogGroupItem;
import com.odde.doughnut.controllers.dto.NotebookCatalogNotebookItem;
import com.odde.doughnut.controllers.dto.NotebookCatalogSubscribedNotebookItem;
import com.odde.doughnut.entities.Notebook;
import com.odde.doughnut.entities.NotebookGroup;
import com.odde.doughnut.entities.Subscription;
import com.odde.doughnut.entities.User;
import com.odde.doughnut.exceptions.UnexpectedNoAccessRightException;
import java.sql.Timestamp;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class NotebookCatalogControllerTest extends NotebookControllerTestBase {

  @BeforeEach
  void freshUser() {
    currentUser.setUser(makeMe.aUser().please());
  }

  @Test
  void groupedNotebookAppearsOnlyInsideGroupRow() throws UnexpectedNoAccessRightException {
    Notebook grouped = ownedNotebook();
    Notebook ungrouped = ownedNotebook();
    User user = currentUser.getUser();
    NotebookGroup group = notebookGroupService.createGroup(user, user.getOwnership(), "G");
    notebookGroupService.assignNotebookToGroup(user, grouped, group);
    var view = controller.myNotebooks();
    assertThat(view.notebooks.size(), equalTo(2));
    assertFalse(
        view.catalogItems.stream()
            .filter(NotebookCatalogNotebookItem.class::isInstance)
            .map(NotebookCatalogNotebookItem.class::cast)
            .anyMatch(cell -> cell.notebook.getId().equals(grouped.getId())));
    NotebookCatalogGroupItem groupRow =
        view.catalogItems.stream()
            .filter(NotebookCatalogGroupItem.class::isInstance)
            .map(NotebookCatalogGroupItem.class::cast)
            .filter(g -> g.id.equals(group.getId()))
            .findFirst()
            .orElseThrow();
    assertThat(
        groupRow.notebooks.stream().map(n -> n.notebook().getId()).toList(),
        equalTo(List.of(grouped.getId())));
    assertThat(
        view.catalogItems.stream()
            .filter(NotebookCatalogNotebookItem.class::isInstance)
            .map(NotebookCatalogNotebookItem.class::cast)
            .map(n -> n.notebook.getId())
            .toList(),
        equalTo(List.of(ungrouped.getId())));
  }

  @Test
  void ungroupedNotebooksOrderedByNotebookUpdatedAt() {
    testabilitySettings.timeTravelTo(Timestamp.valueOf("2020-01-01 00:00:00"));
    Notebook first = ownedNotebook();
    testabilitySettings.timeTravelTo(Timestamp.valueOf("2020-06-01 00:00:00"));
    Notebook second = ownedNotebook();
    var view = controller.myNotebooks();
    assertThat(
        view.catalogItems.stream()
            .filter(NotebookCatalogNotebookItem.class::isInstance)
            .map(NotebookCatalogNotebookItem.class::cast)
            .map(n -> n.notebook.getId())
            .toList(),
        equalTo(List.of(first.getId(), second.getId())));
  }

  @Test
  void subscribedNotebookInGroupAppearsOnlyInsideGroupRow()
      throws UnexpectedNoAccessRightException {
    User subscriber = currentUser.getUser();
    Notebook bazaarNotebook = makeMe.aNotebook().creatorAndOwner(makeMe.aUser().please()).please();
    makeMe.aBazaarNotebook(bazaarNotebook).please();
    Subscription subscription =
        makeMe.aSubscription().forNotebook(bazaarNotebook).forUser(subscriber).please();
    NotebookGroup group =
        notebookGroupService.createGroup(subscriber, subscriber.getOwnership(), "G");
    notebookGroupService.assignSubscriptionToGroup(subscriber, subscription, group);
    makeMe.refresh(subscriber);
    var view = controller.myNotebooks();
    assertFalse(
        view.catalogItems.stream()
            .anyMatch(
                item ->
                    item instanceof NotebookCatalogSubscribedNotebookItem s
                        && s.notebook.getId().equals(bazaarNotebook.getId())));
    NotebookCatalogGroupItem groupRow =
        view.catalogItems.stream()
            .filter(NotebookCatalogGroupItem.class::isInstance)
            .map(NotebookCatalogGroupItem.class::cast)
            .filter(g -> g.id.equals(group.getId()))
            .findFirst()
            .orElseThrow();
    assertThat(
        groupRow.notebooks.stream().map(n -> n.notebook().getId()).toList(),
        equalTo(List.of(bazaarNotebook.getId())));
  }

  @Test
  void subscribedNotebookAppearsInCatalogItemsBetweenOwnedRows() {
    User subscriber = currentUser.getUser();
    User owner = makeMe.aUser().please();
    testabilitySettings.timeTravelTo(Timestamp.valueOf("2020-01-01 00:00:00"));
    Notebook first = ownedNotebook();
    testabilitySettings.timeTravelTo(Timestamp.valueOf("2020-06-01 00:00:00"));
    Notebook bazaarNotebook = makeMe.aNotebook().creatorAndOwner(owner).please();
    makeMe.aBazaarNotebook(bazaarNotebook).please();
    Subscription subscription =
        makeMe.aSubscription().forNotebook(bazaarNotebook).forUser(subscriber).please();
    testabilitySettings.timeTravelTo(Timestamp.valueOf("2020-12-01 00:00:00"));
    Notebook second = ownedNotebook();
    makeMe.refresh(subscriber);
    var view = controller.myNotebooks();
    assertThat(
        view.catalogItems.stream().map(NotebookControllerTestBase::catalogItemNotebookId).toList(),
        equalTo(List.of(first.getId(), bazaarNotebook.getId(), second.getId())));
    NotebookCatalogSubscribedNotebookItem subscribedRow =
        view.catalogItems.stream()
            .filter(NotebookCatalogSubscribedNotebookItem.class::isInstance)
            .map(NotebookCatalogSubscribedNotebookItem.class::cast)
            .findFirst()
            .orElseThrow();
    assertThat(subscribedRow.subscriptionId, equalTo(subscription.getId()));
  }

  @Test
  void setsHasAttachedBookOnCatalogNotebooks() {
    Notebook withBook =
        makeMe
            .aNotebook()
            .creatorAndOwner(currentUser.getUser())
            .withBook("Attached Title")
            .please();
    Notebook withoutBook = ownedNotebook();
    var view = controller.myNotebooks();
    assertThat(view.notebooks.size(), equalTo(2));
    NotebookCatalogNotebookItem withRow =
        view.catalogItems.stream()
            .filter(NotebookCatalogNotebookItem.class::isInstance)
            .map(NotebookCatalogNotebookItem.class::cast)
            .filter(n -> n.notebook.getId().equals(withBook.getId()))
            .findFirst()
            .orElseThrow();
    assertThat(withRow.hasAttachedBook, equalTo(true));
    NotebookCatalogNotebookItem withoutRow =
        view.catalogItems.stream()
            .filter(NotebookCatalogNotebookItem.class::isInstance)
            .map(NotebookCatalogNotebookItem.class::cast)
            .filter(n -> n.notebook.getId().equals(withoutBook.getId()))
            .findFirst()
            .orElseThrow();
    assertThat(withoutRow.hasAttachedBook, equalTo(false));
  }
}

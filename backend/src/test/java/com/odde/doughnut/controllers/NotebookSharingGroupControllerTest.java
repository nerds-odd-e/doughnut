package com.odde.doughnut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.odde.doughnut.controllers.dto.NotebookCatalogGroupItem;
import com.odde.doughnut.controllers.dto.NotebookCatalogNotebookItem;
import com.odde.doughnut.controllers.dto.NotebookCatalogSubscribedNotebookItem;
import com.odde.doughnut.controllers.dto.UpdateAiAssistantRequest;
import com.odde.doughnut.controllers.dto.UpdateNotebookGroupRequest;
import com.odde.doughnut.entities.Circle;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.Notebook;
import com.odde.doughnut.entities.NotebookAiAssistant;
import com.odde.doughnut.entities.NotebookGroup;
import com.odde.doughnut.entities.Subscription;
import com.odde.doughnut.entities.User;
import com.odde.doughnut.exceptions.UnexpectedNoAccessRightException;
import java.sql.Timestamp;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

class NotebookSharingGroupControllerTest extends NotebookControllerTestBase {

  @Nested
  class ShareMyNotebook {

    @Test
    void shareMyNote() throws UnexpectedNoAccessRightException {
      long oldCount = bazaarNotebookRepository.count();
      controller.shareNotebook(topNote.getNotebook());
      assertThat(bazaarNotebookRepository.count(), equalTo(oldCount + 1));
    }

    @Test
    void shouldNotBeAbleToShareNoteThatBelongsToOtherUser() {
      User anotherUser = makeMe.aUser().please();
      Note note = makeMe.aNote().notebookOwnedBy(anotherUser).please();
      assertThrows(
          UnexpectedNoAccessRightException.class,
          () -> controller.shareNotebook(note.getNotebook()));
    }
  }

  @Nested
  class MoveToCircle {
    @Test
    void shouldNotBeAbleToMoveNotebookThatIsCreatedByAnotherUser() {
      User anotherUser = makeMe.aUser().please();
      Circle circle1 =
          makeMe.aCircle().hasMember(anotherUser).hasMember(currentUser.getUser()).please();
      Note note = makeMe.aNote().toBeRemoved(anotherUser).inCircle(circle1).please();
      assertThrows(
          UnexpectedNoAccessRightException.class,
          () -> controller.moveToCircle(note.getNotebook(), makeMe.aCircle().please()));
    }
  }

  @Nested
  class UpdateNotebookGroup {
    @Test
    void assignsNotebookToGroup() throws UnexpectedNoAccessRightException {
      User user = makeMe.aUser().please();
      currentUser.setUser(user);
      Notebook notebook = makeMe.aNotebook().creatorAndOwner(user).please();
      NotebookGroup group = notebookGroupService.createGroup(user, user.getOwnership(), "G");
      UpdateNotebookGroupRequest req = new UpdateNotebookGroupRequest();
      req.setNotebookGroupId(group.getId());
      controller.updateNotebookGroup(notebook, req);
      assertThat(notebook.getNotebookGroup().getId(), equalTo(group.getId()));
    }

    @Test
    void reassignsToAnotherGroup() throws UnexpectedNoAccessRightException {
      User user = makeMe.aUser().please();
      currentUser.setUser(user);
      Notebook notebook = makeMe.aNotebook().creatorAndOwner(user).please();
      NotebookGroup g1 = notebookGroupService.createGroup(user, user.getOwnership(), "G1");
      NotebookGroup g2 = notebookGroupService.createGroup(user, user.getOwnership(), "G2");
      UpdateNotebookGroupRequest req1 = new UpdateNotebookGroupRequest();
      req1.setNotebookGroupId(g1.getId());
      controller.updateNotebookGroup(notebook, req1);
      UpdateNotebookGroupRequest req2 = new UpdateNotebookGroupRequest();
      req2.setNotebookGroupId(g2.getId());
      controller.updateNotebookGroup(notebook, req2);
      assertThat(notebook.getNotebookGroup().getId(), equalTo(g2.getId()));
    }

    @Test
    void clearsGroupWhenNotebookGroupIdIsNull() throws UnexpectedNoAccessRightException {
      User user = makeMe.aUser().please();
      currentUser.setUser(user);
      Notebook notebook = makeMe.aNotebook().creatorAndOwner(user).please();
      NotebookGroup group = notebookGroupService.createGroup(user, user.getOwnership(), "G");
      notebookGroupService.assignNotebookToGroup(user, notebook, group);
      UpdateNotebookGroupRequest req = new UpdateNotebookGroupRequest();
      req.setNotebookGroupId(null);
      controller.updateNotebookGroup(notebook, req);
      assertThat(notebook.getNotebookGroup(), equalTo(null));
    }

    @Test
    void rejectsNotebookOwnedByAnotherUser() throws UnexpectedNoAccessRightException {
      User owner = makeMe.aUser().please();
      currentUser.setUser(owner);
      NotebookGroup group = notebookGroupService.createGroup(owner, owner.getOwnership(), "G");
      User other = makeMe.aUser().please();
      Notebook otherNotebook = makeMe.aNotebook().creatorAndOwner(other).please();
      UpdateNotebookGroupRequest req = new UpdateNotebookGroupRequest();
      req.setNotebookGroupId(group.getId());
      assertThrows(
          UnexpectedNoAccessRightException.class,
          () -> controller.updateNotebookGroup(otherNotebook, req));
    }

    @Test
    void notFoundWhenGroupDoesNotExist() {
      User user = makeMe.aUser().please();
      currentUser.setUser(user);
      Notebook notebook = makeMe.aNotebook().creatorAndOwner(user).please();
      UpdateNotebookGroupRequest req = new UpdateNotebookGroupRequest();
      req.setNotebookGroupId(9_999_999);
      ResponseStatusException ex =
          assertThrows(
              ResponseStatusException.class, () -> controller.updateNotebookGroup(notebook, req));
      assertThat(ex.getStatusCode().value(), equalTo(HttpStatus.NOT_FOUND.value()));
    }

    @Test
    void rejectsGroupFromAnotherOwnership() throws UnexpectedNoAccessRightException {
      User owner = makeMe.aUser().please();
      User other = makeMe.aUser().please();
      currentUser.setUser(owner);
      Notebook notebook = makeMe.aNotebook().creatorAndOwner(owner).please();
      NotebookGroup otherGroup = notebookGroupService.createGroup(other, other.getOwnership(), "G");
      UpdateNotebookGroupRequest req = new UpdateNotebookGroupRequest();
      req.setNotebookGroupId(otherGroup.getId());
      assertThrows(
          UnexpectedNoAccessRightException.class,
          () -> controller.updateNotebookGroup(notebook, req));
    }
  }

  @Nested
  class MyNotebooksCatalog {
    @Test
    void groupedNotebookAppearsOnlyInsideGroupRow() throws UnexpectedNoAccessRightException {
      User user = makeMe.aUser().please();
      currentUser.setUser(user);
      Notebook grouped = makeMe.aNotebook().creatorAndOwner(user).please();
      Notebook ungrouped = makeMe.aNotebook().creatorAndOwner(user).please();
      NotebookGroup group = notebookGroupService.createGroup(user, user.getOwnership(), "G");
      notebookGroupService.assignNotebookToGroup(user, grouped, group);
      var view = controller.myNotebooks();
      assertThat(view.notebooks.size(), equalTo(2));
      boolean topLevelGrouped =
          view.catalogItems.stream()
              .filter(NotebookCatalogNotebookItem.class::isInstance)
              .map(NotebookCatalogNotebookItem.class::cast)
              .anyMatch(cell -> cell.notebook.getId().equals(grouped.getId()));
      assertFalse(topLevelGrouped);
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
      User user = makeMe.aUser().please();
      currentUser.setUser(user);
      testabilitySettings.timeTravelTo(Timestamp.valueOf("2020-01-01 00:00:00"));
      Notebook first = makeMe.aNotebook().creatorAndOwner(user).please();
      testabilitySettings.timeTravelTo(Timestamp.valueOf("2020-06-01 00:00:00"));
      Notebook second = makeMe.aNotebook().creatorAndOwner(user).please();
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
      User subscriber = makeMe.aUser().please();
      currentUser.setUser(subscriber);
      User owner = makeMe.aUser().please();
      Notebook bazaarNotebook = makeMe.aNotebook().creatorAndOwner(owner).please();
      makeMe.aBazaarNotebook(bazaarNotebook).please();
      Subscription subscription =
          makeMe.aSubscription().forNotebook(bazaarNotebook).forUser(subscriber).please();
      NotebookGroup group =
          notebookGroupService.createGroup(subscriber, subscriber.getOwnership(), "G");
      notebookGroupService.assignSubscriptionToGroup(subscriber, subscription, group);
      makeMe.refresh(subscriber);
      var view = controller.myNotebooks();
      boolean topLevelSubscribed =
          view.catalogItems.stream()
              .anyMatch(
                  item ->
                      item instanceof NotebookCatalogSubscribedNotebookItem s
                          && s.notebook.getId().equals(bazaarNotebook.getId()));
      assertFalse(topLevelSubscribed);
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
      User subscriber = makeMe.aUser().please();
      currentUser.setUser(subscriber);
      User owner = makeMe.aUser().please();
      testabilitySettings.timeTravelTo(Timestamp.valueOf("2020-01-01 00:00:00"));
      Notebook first = makeMe.aNotebook().creatorAndOwner(subscriber).please();
      testabilitySettings.timeTravelTo(Timestamp.valueOf("2020-06-01 00:00:00"));
      Notebook bazaarNotebook = makeMe.aNotebook().creatorAndOwner(owner).please();
      makeMe.aBazaarNotebook(bazaarNotebook).please();
      Subscription subscription =
          makeMe.aSubscription().forNotebook(bazaarNotebook).forUser(subscriber).please();
      testabilitySettings.timeTravelTo(Timestamp.valueOf("2020-12-01 00:00:00"));
      Notebook second = makeMe.aNotebook().creatorAndOwner(subscriber).please();
      makeMe.refresh(subscriber);
      var view = controller.myNotebooks();
      var catalogIds =
          view.catalogItems.stream()
              .map(NotebookControllerTestBase::catalogItemNotebookId)
              .toList();
      assertThat(
          catalogIds, equalTo(List.of(first.getId(), bazaarNotebook.getId(), second.getId())));
      NotebookCatalogSubscribedNotebookItem subscribedRow =
          view.catalogItems.stream()
              .filter(NotebookCatalogSubscribedNotebookItem.class::isInstance)
              .map(NotebookCatalogSubscribedNotebookItem.class::cast)
              .findFirst()
              .orElseThrow();
      assertThat(subscribedRow.subscriptionId, equalTo(subscription.getId()));
    }

    @Test
    void myNotebooks_setsHasAttachedBookOnCatalogNotebooks() {
      User user = makeMe.aUser().please();
      currentUser.setUser(user);
      Notebook withBook =
          makeMe.aNotebook().creatorAndOwner(user).withBook("Attached Title").please();
      Notebook withoutBook = makeMe.aNotebook().creatorAndOwner(user).please();
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

  @Nested
  class UpdateNotebookAiAssistant {
    private Notebook notebook;

    @BeforeEach
    void setup() {
      testabilitySettings.timeTravelTo(makeMe.aTimestamp().please());
      notebook = makeMe.aNotebook().creatorAndOwner(currentUser.getUser()).please();
    }

    @Test
    void shouldCreateNewAiAssistantWhenNotExists() throws UnexpectedNoAccessRightException {
      String instructions = "Some AI instructions";
      UpdateAiAssistantRequest request = new UpdateAiAssistantRequest();
      request.setAdditionalInstructions(instructions);

      NotebookAiAssistant result = controller.updateAiAssistant(notebook, request);

      assertThat(result.getNotebook().getId(), equalTo(notebook.getId()));
      assertThat(result.getAdditionalInstructionsToAi(), equalTo(instructions));
      assertThat(result.getCreatedAt(), equalTo(testabilitySettings.getCurrentUTCTimestamp()));
      assertThat(result.getUpdatedAt(), equalTo(testabilitySettings.getCurrentUTCTimestamp()));
    }

    @Test
    void shouldUpdateExistingAiAssistant() throws UnexpectedNoAccessRightException {
      String initialInstructions = "Initial instructions";
      UpdateAiAssistantRequest initialRequest = new UpdateAiAssistantRequest();
      initialRequest.setAdditionalInstructions(initialInstructions);
      NotebookAiAssistant initial = controller.updateAiAssistant(notebook, initialRequest);

      String newInstructions = "New instructions";
      UpdateAiAssistantRequest newRequest = new UpdateAiAssistantRequest();
      newRequest.setAdditionalInstructions(newInstructions);
      NotebookAiAssistant result = controller.updateAiAssistant(notebook, newRequest);

      assertThat(result.getId(), equalTo(initial.getId()));
      assertThat(result.getAdditionalInstructionsToAi(), equalTo(newInstructions));
      assertThat(result.getCreatedAt(), equalTo(initial.getCreatedAt()));
      assertThat(result.getUpdatedAt(), equalTo(testabilitySettings.getCurrentUTCTimestamp()));
    }

    @Test
    void shouldNotAllowUnauthorizedUpdate() {
      User anotherUser = makeMe.aUser().please();
      Note note = makeMe.aNote().notebookOwnedBy(anotherUser).please();

      UpdateAiAssistantRequest request = new UpdateAiAssistantRequest();
      request.setAdditionalInstructions("Some instructions");

      assertThrows(
          UnexpectedNoAccessRightException.class,
          () -> controller.updateAiAssistant(note.getNotebook(), request));
    }
  }

  @Nested
  class GetNotebookAiAssistant {
    private Notebook notebook;

    @BeforeEach
    void setup() {
      testabilitySettings.timeTravelTo(makeMe.aTimestamp().please());
      notebook = makeMe.aNotebook().creatorAndOwner(currentUser.getUser()).please();
    }

    @Test
    void shouldReturnNullWhenAssistantNotExists() throws UnexpectedNoAccessRightException {
      NotebookAiAssistant result = controller.getAiAssistant(notebook);
      assertThat(result, equalTo(null));
    }

    @Test
    void shouldReturnExistingAssistant() throws UnexpectedNoAccessRightException {
      String instructions = "Initial instructions";
      UpdateAiAssistantRequest request = new UpdateAiAssistantRequest();
      request.setAdditionalInstructions(instructions);
      NotebookAiAssistant created = controller.updateAiAssistant(notebook, request);

      NotebookAiAssistant result = controller.getAiAssistant(notebook);
      assertThat(result.getId(), equalTo(created.getId()));
      assertThat(result.getAdditionalInstructionsToAi(), equalTo(instructions));
    }

    @Test
    void shouldNotAllowUnauthorizedAccess() {
      User anotherUser = makeMe.aUser().please();
      Note note = makeMe.aNote().notebookOwnedBy(anotherUser).please();

      assertThrows(
          UnexpectedNoAccessRightException.class,
          () -> controller.getAiAssistant(note.getNotebook()));
    }
  }
}

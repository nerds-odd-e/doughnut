package com.odde.donut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.odde.donut.controllers.dto.CircleForUserView;
import com.odde.donut.controllers.dto.CircleJoiningByInvitation;
import com.odde.donut.controllers.dto.NotebookCatalogGroupItem;
import com.odde.donut.controllers.dto.NotebookCatalogNotebookItem;
import com.odde.donut.controllers.dto.NotebookCreationRequest;
import com.odde.donut.controllers.dto.NotebookRealm;
import com.odde.donut.entities.Circle;
import com.odde.donut.entities.Notebook;
import com.odde.donut.entities.NotebookGroup;
import com.odde.donut.entities.User;
import com.odde.donut.entities.repositories.NoteRepository;
import com.odde.donut.entities.repositories.NotebookGitBindingRepository;
import com.odde.donut.entities.repositories.NotebookRepository;
import com.odde.donut.exceptions.UnexpectedNoAccessRightException;
import com.odde.donut.services.NotebookGroupService;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.BindException;
import org.springframework.web.server.ResponseStatusException;

class CircleControllerTest extends ControllerTestBase {
  @Autowired CircleController controller;
  @Autowired NotebookGroupService notebookGroupService;
  @Autowired NoteRepository noteRepository;
  @Autowired NotebookRepository notebookRepository;
  @Autowired NotebookGitBindingRepository notebookGitBindingRepository;

  @BeforeEach
  void setup() {
    currentUser.setUser(makeMe.aUser().please());
  }

  @Nested
  class CircleIndex {
    @Test
    void requiresLogin() {
      currentUser.setUser(null);
      assertThrows(ResponseStatusException.class, () -> controller.index());
    }
  }

  @Nested
  class CreateNotebookInCircle {
    @Test
    void nonMemberDenied() {
      Circle circle = makeMe.aCircle().please();
      NotebookCreationRequest noteCreation = new NotebookCreationRequest();
      noteCreation.setNewTitle("new title");
      assertThrows(
          UnexpectedNoAccessRightException.class,
          () -> controller.createNotebookInCircle(circle, noteCreation));
    }

    @Test
    void persistsDescriptionWhenMember() throws UnexpectedNoAccessRightException {
      User user = currentUser.getUser();
      Circle circle = makeMe.aCircle().hasMember(user).please();
      NotebookCreationRequest noteCreation = new NotebookCreationRequest();
      noteCreation.setNewTitle("Circle Owned Nb");
      noteCreation.setDescription("Circle catalog blurb");

      NotebookRealm response = controller.createNotebookInCircle(circle, noteCreation);

      assertThat(response.notebook().getId(), notNullValue());
      Notebook nb = notebookRepository.findById(response.notebook().getId()).orElseThrow();
      assertThat(nb.getDescription(), equalTo("Circle catalog blurb"));
      assertThat(
          noteRepository.findNotesInNotebookRootFolderScopeByNotebookId(
              response.notebook().getId()),
          empty());
    }

    @Test
    void assignsToCircleGroupWhenNotebookGroupIdGiven() throws UnexpectedNoAccessRightException {
      User user = currentUser.getUser();
      Circle circle = makeMe.aCircle().hasMember(user).please();
      NotebookGroup group =
          notebookGroupService.createGroup(user, circle.getOwnership(), "Circle create group");
      NotebookCreationRequest noteCreation = new NotebookCreationRequest();
      noteCreation.setNewTitle("In Circle Group");
      noteCreation.setNotebookGroupId(group.getId());

      NotebookRealm response = controller.createNotebookInCircle(circle, noteCreation);

      Notebook nb = notebookRepository.findById(response.notebook().getId()).orElseThrow();
      assertThat(nb.getNotebookGroup().getId(), equalTo(group.getId()));
    }

    @Test
    void startsWithAnEmptyTreeRootCommitBinding() throws Exception {
      User user = currentUser.getUser();
      Circle circle = makeMe.aCircle().hasMember(user).please();
      NotebookCreationRequest noteCreation = new NotebookCreationRequest();
      noteCreation.setNewTitle("Circle Git Backed Nb");

      NotebookRealm response = controller.createNotebookInCircle(circle, noteCreation);

      NotebookGitBindingAssertions.assertEmptyTreeRootCommitBinding(
          notebookGitBindingRepository, response.notebook().getId());
    }
  }

  @Nested
  class ShowCircle {
    @Test
    void returnsCircleForMember() throws UnexpectedNoAccessRightException {
      Circle circle = makeMe.aCircle().hasMember(currentUser.getUser()).please();

      CircleForUserView actual = controller.showCircle(circle);

      assertThat(actual.getId(), equalTo(circle.getId()));
      assertThat(actual.getName(), equalTo(circle.getName()));
      assertThat(actual.getInvitationCode(), equalTo(circle.getInvitationCode()));
    }

    @Test
    void requiresLogin() {
      Circle circle = makeMe.aCircle().please();
      currentUser.setUser(null);
      assertThrows(ResponseStatusException.class, () -> controller.showCircle(circle));
    }

    @Test
    void nonMemberDenied() {
      Circle circle = makeMe.aCircle().please();
      assertThrows(UnexpectedNoAccessRightException.class, () -> controller.showCircle(circle));
    }

    @Test
    void notebooksViewIncludesCatalogWithCircleOwnedGroups()
        throws UnexpectedNoAccessRightException {
      User user = currentUser.getUser();
      Circle circle = makeMe.aCircle().hasMember(user).please();
      Notebook inGroup = makeMe.aNotebook().creatorAndOwner(user).owner(circle).please();
      Notebook ungrouped = makeMe.aNotebook().creatorAndOwner(user).owner(circle).please();
      NotebookGroup group =
          notebookGroupService.createGroup(user, circle.getOwnership(), "Circle group");
      notebookGroupService.assignNotebookToGroup(user, inGroup, group);

      var notebooksView = controller.showCircle(circle).getNotebooks();

      assertThat(notebooksView.notebooks.size(), equalTo(2));
      assertFalse(
          notebooksView.catalogItems.stream()
              .filter(NotebookCatalogNotebookItem.class::isInstance)
              .map(NotebookCatalogNotebookItem.class::cast)
              .anyMatch(row -> row.notebook.getId().equals(inGroup.getId())));
      NotebookCatalogGroupItem groupRow =
          notebooksView.catalogItems.stream()
              .filter(NotebookCatalogGroupItem.class::isInstance)
              .map(NotebookCatalogGroupItem.class::cast)
              .filter(gr -> gr.id.equals(group.getId()))
              .findFirst()
              .orElseThrow();
      assertThat(
          groupRow.notebooks.stream().map(n -> n.notebook().getId()).toList(),
          equalTo(List.of(inGroup.getId())));
      assertThat(
          notebooksView.catalogItems.stream()
              .filter(NotebookCatalogNotebookItem.class::isInstance)
              .map(NotebookCatalogNotebookItem.class::cast)
              .map(row -> row.notebook.getId())
              .toList(),
          equalTo(List.of(ungrouped.getId())));
    }
  }

  @Nested
  class JoinCircle {
    @Test
    void validationFailed() {
      CircleJoiningByInvitation entity = new CircleJoiningByInvitation();
      entity.setInvitationCode("short");
      BindException exception =
          assertThrows(BindException.class, () -> controller.joinCircle(entity));
      assertThat(exception.getErrorCount(), equalTo(1));
    }

    @Test
    void userAlreadyInCircle() {
      Circle circle = makeMe.aCircle().hasMember(currentUser.getUser()).please();
      CircleJoiningByInvitation entity = new CircleJoiningByInvitation();
      entity.setInvitationCode(circle.getInvitationCode());
      BindException exception =
          assertThrows(BindException.class, () -> controller.joinCircle(entity));
      assertThat(exception.getErrorCount(), equalTo(1));
    }
  }
}

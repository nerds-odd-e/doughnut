package com.odde.donut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.odde.donut.controllers.dto.UpdateNotebookGroupRequest;
import com.odde.donut.entities.Circle;
import com.odde.donut.entities.Note;
import com.odde.donut.entities.Notebook;
import com.odde.donut.entities.NotebookGroup;
import com.odde.donut.entities.User;
import com.odde.donut.exceptions.UnexpectedNoAccessRightException;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

class NotebookSharingGroupControllerTest extends NotebookControllerTestBase {

  private UpdateNotebookGroupRequest groupRequest(Integer notebookGroupId) {
    UpdateNotebookGroupRequest req = new UpdateNotebookGroupRequest();
    req.setNotebookGroupId(notebookGroupId);
    return req;
  }

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
      Note note = makeMe.aNote().notebookOwnedBy(makeMe.aUser().please()).please();
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
      Note note = makeMe.aNote().inCircle(circle1).please();
      assertThrows(
          UnexpectedNoAccessRightException.class,
          () -> controller.moveToCircle(note.getNotebook(), makeMe.aCircle().please()));
    }
  }

  @Nested
  class UpdateNotebookGroup {
    @Test
    void assignsNotebookToGroup() throws UnexpectedNoAccessRightException {
      Notebook notebook = ownedNotebook();
      NotebookGroup group =
          notebookGroupService.createGroup(
              currentUser.getUser(), currentUser.getUser().getOwnership(), "G");
      controller.updateNotebookGroup(notebook, groupRequest(group.getId()));
      assertThat(notebook.getNotebookGroup().getId(), equalTo(group.getId()));
    }

    @Test
    void reassignsToAnotherGroup() throws UnexpectedNoAccessRightException {
      Notebook notebook = ownedNotebook();
      User user = currentUser.getUser();
      NotebookGroup g1 = notebookGroupService.createGroup(user, user.getOwnership(), "G1");
      NotebookGroup g2 = notebookGroupService.createGroup(user, user.getOwnership(), "G2");
      controller.updateNotebookGroup(notebook, groupRequest(g1.getId()));
      controller.updateNotebookGroup(notebook, groupRequest(g2.getId()));
      assertThat(notebook.getNotebookGroup().getId(), equalTo(g2.getId()));
    }

    @Test
    void clearsGroupWhenNotebookGroupIdIsNull() throws UnexpectedNoAccessRightException {
      Notebook notebook = ownedNotebook();
      User user = currentUser.getUser();
      NotebookGroup group = notebookGroupService.createGroup(user, user.getOwnership(), "G");
      notebookGroupService.assignNotebookToGroup(user, notebook, group);
      controller.updateNotebookGroup(notebook, groupRequest(null));
      assertThat(notebook.getNotebookGroup(), nullValue());
    }

    @Test
    void rejectsNotebookOwnedByAnotherUser() throws UnexpectedNoAccessRightException {
      User owner = currentUser.getUser();
      NotebookGroup group = notebookGroupService.createGroup(owner, owner.getOwnership(), "G");
      Notebook otherNotebook = makeMe.aNotebook().creatorAndOwner(makeMe.aUser().please()).please();
      assertThrows(
          UnexpectedNoAccessRightException.class,
          () -> controller.updateNotebookGroup(otherNotebook, groupRequest(group.getId())));
    }

    @Test
    void notFoundWhenGroupDoesNotExist() {
      ResponseStatusException ex =
          assertThrows(
              ResponseStatusException.class,
              () -> controller.updateNotebookGroup(ownedNotebook(), groupRequest(9_999_999)));
      assertThat(ex.getStatusCode().value(), equalTo(HttpStatus.NOT_FOUND.value()));
    }

    @Test
    void rejectsGroupFromAnotherOwnership() throws UnexpectedNoAccessRightException {
      User other = makeMe.aUser().please();
      NotebookGroup otherGroup = notebookGroupService.createGroup(other, other.getOwnership(), "G");
      assertThrows(
          UnexpectedNoAccessRightException.class,
          () -> controller.updateNotebookGroup(ownedNotebook(), groupRequest(otherGroup.getId())));
    }
  }
}

package com.odde.donut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.odde.donut.controllers.dto.CreateNotebookGroupRequest;
import com.odde.donut.entities.Circle;
import com.odde.donut.exceptions.UnexpectedNoAccessRightException;
import com.odde.donut.services.CircleService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.server.ResponseStatusException;

class NotebookGroupControllerTest extends ControllerTestBase {

  @Autowired NotebookGroupController notebookGroupController;
  @Autowired CircleService circleService;

  @BeforeEach
  void login() {
    currentUser.setUser(makeMe.aUser().please());
  }

  private CreateNotebookGroupRequest groupCreate(String name) {
    CreateNotebookGroupRequest req = new CreateNotebookGroupRequest();
    req.setName(name);
    return req;
  }

  @Test
  void createsGroupForCurrentUser() throws UnexpectedNoAccessRightException {
    var group = notebookGroupController.createGroup(groupCreate("Alpha"));
    makeMe.refresh(group);
    assertThat(group.getName(), equalTo("Alpha"));
    assertThat(group.getOwnership().getId(), equalTo(currentUser.getUser().getOwnership().getId()));
  }

  @Test
  void rejectsWhenNotLoggedIn() {
    currentUser.setUser(null);
    assertThrows(
        ResponseStatusException.class, () -> notebookGroupController.createGroup(groupCreate("X")));
  }

  @Test
  void createsGroupOnCircleOwnershipWhenCircleIdSet() throws UnexpectedNoAccessRightException {
    Circle circle = makeMe.aCircle().please();
    circleService.joinAndSave(circle, currentUser.getUser());
    CreateNotebookGroupRequest req = groupCreate("Circle group");
    req.setCircleId(circle.getId());
    var group = notebookGroupController.createGroup(req);
    makeMe.refresh(group);
    assertThat(group.getOwnership().getId(), equalTo(circle.getOwnership().getId()));
  }

  @Test
  void rejectsCircleGroupWhenNotMember() {
    Circle circle = makeMe.aCircle().please();
    CreateNotebookGroupRequest req = groupCreate("X");
    req.setCircleId(circle.getId());
    assertThrows(
        UnexpectedNoAccessRightException.class, () -> notebookGroupController.createGroup(req));
  }
}

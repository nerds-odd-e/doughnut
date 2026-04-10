package com.odde.doughnut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.odde.doughnut.controllers.dto.CreateNotebookGroupRequest;
import com.odde.doughnut.entities.Circle;
import com.odde.doughnut.exceptions.UnexpectedNoAccessRightException;
import com.odde.doughnut.services.CircleService;
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

  @Test
  void createsGroupForCurrentUser() throws UnexpectedNoAccessRightException {
    CreateNotebookGroupRequest req = new CreateNotebookGroupRequest();
    req.setName("Alpha");
    var group = notebookGroupController.createGroup(req);
    makeMe.refresh(group);
    assertThat(group.getName(), equalTo("Alpha"));
    assertThat(group.getOwnership().getId(), equalTo(currentUser.getUser().getOwnership().getId()));
  }

  @Test
  void rejectsWhenNotLoggedIn() {
    currentUser.setUser(null);
    CreateNotebookGroupRequest req = new CreateNotebookGroupRequest();
    req.setName("X");
    assertThrows(ResponseStatusException.class, () -> notebookGroupController.createGroup(req));
  }

  @Test
  void createsGroupOnCircleOwnershipWhenCircleIdSet() throws UnexpectedNoAccessRightException {
    Circle circle = makeMe.aCircle().please();
    circleService.joinAndSave(circle, currentUser.getUser());
    CreateNotebookGroupRequest req = new CreateNotebookGroupRequest();
    req.setName("Circle group");
    req.setCircleId(circle.getId());
    var group = notebookGroupController.createGroup(req);
    makeMe.refresh(group);
    assertThat(group.getName(), equalTo("Circle group"));
    assertThat(group.getOwnership().getId(), equalTo(circle.getOwnership().getId()));
  }

  @Test
  void rejectsCircleGroupWhenNotMember() {
    Circle circle = makeMe.aCircle().please();
    CreateNotebookGroupRequest req = new CreateNotebookGroupRequest();
    req.setName("X");
    req.setCircleId(circle.getId());
    assertThrows(
        UnexpectedNoAccessRightException.class, () -> notebookGroupController.createGroup(req));
  }
}

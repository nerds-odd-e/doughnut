package com.odde.doughnut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.odde.doughnut.controllers.dto.CreateNotebookGroupRequest;
import com.odde.doughnut.exceptions.UnexpectedNoAccessRightException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.server.ResponseStatusException;

class NotebookGroupControllerTest extends ControllerTestBase {

  @Autowired NotebookGroupController notebookGroupController;

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
}

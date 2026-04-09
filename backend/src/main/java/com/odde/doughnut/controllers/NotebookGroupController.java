package com.odde.doughnut.controllers;

import com.odde.doughnut.controllers.dto.CreateNotebookGroupRequest;
import com.odde.doughnut.entities.NotebookGroup;
import com.odde.doughnut.entities.User;
import com.odde.doughnut.exceptions.UnexpectedNoAccessRightException;
import com.odde.doughnut.services.AuthorizationService;
import com.odde.doughnut.services.NotebookGroupService;
import jakarta.validation.Valid;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.annotation.SessionScope;

@RestController
@SessionScope
@RequestMapping("/api/notebook-groups")
class NotebookGroupController {
  private final NotebookGroupService notebookGroupService;
  private final AuthorizationService authorizationService;

  NotebookGroupController(
      NotebookGroupService notebookGroupService, AuthorizationService authorizationService) {
    this.notebookGroupService = notebookGroupService;
    this.authorizationService = authorizationService;
  }

  @PostMapping("")
  @Transactional
  public NotebookGroup createGroup(@Valid @RequestBody CreateNotebookGroupRequest request)
      throws UnexpectedNoAccessRightException {
    authorizationService.assertLoggedIn();
    User user = authorizationService.getCurrentUser();
    return notebookGroupService.createGroup(user, user.getOwnership(), request.getName());
  }
}

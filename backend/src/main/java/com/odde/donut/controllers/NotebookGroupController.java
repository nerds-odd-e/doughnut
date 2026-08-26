package com.odde.donut.controllers;

import com.odde.donut.controllers.dto.CreateNotebookGroupRequest;
import com.odde.donut.entities.Circle;
import com.odde.donut.entities.NotebookGroup;
import com.odde.donut.entities.User;
import com.odde.donut.entities.repositories.CircleRepository;
import com.odde.donut.exceptions.UnexpectedNoAccessRightException;
import com.odde.donut.services.AuthorizationService;
import com.odde.donut.services.NotebookGroupService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.annotation.SessionScope;
import org.springframework.web.server.ResponseStatusException;

@RestController
@SessionScope
@RequestMapping("/api/notebook-groups")
class NotebookGroupController {
  private final NotebookGroupService notebookGroupService;
  private final AuthorizationService authorizationService;
  private final CircleRepository circleRepository;

  NotebookGroupController(
      NotebookGroupService notebookGroupService,
      AuthorizationService authorizationService,
      CircleRepository circleRepository) {
    this.notebookGroupService = notebookGroupService;
    this.authorizationService = authorizationService;
    this.circleRepository = circleRepository;
  }

  @PostMapping("")
  @Transactional
  public NotebookGroup createGroup(@Valid @RequestBody CreateNotebookGroupRequest request)
      throws UnexpectedNoAccessRightException {
    authorizationService.assertLoggedIn();
    User user = authorizationService.getCurrentUser();
    Integer circleId = request.getCircleId();
    if (circleId != null) {
      Circle circle =
          circleRepository
              .findById(circleId)
              .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
      authorizationService.assertAuthorization(circle);
      return notebookGroupService.createGroup(user, circle.getOwnership(), request.getName());
    }
    return notebookGroupService.createGroup(user, user.getOwnership(), request.getName());
  }
}

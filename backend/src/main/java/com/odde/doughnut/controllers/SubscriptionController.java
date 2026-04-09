package com.odde.doughnut.controllers;

import com.odde.doughnut.controllers.dto.SubscriptionDTO;
import com.odde.doughnut.controllers.dto.UpdateNotebookGroupRequest;
import com.odde.doughnut.entities.Notebook;
import com.odde.doughnut.entities.NotebookGroup;
import com.odde.doughnut.entities.Subscription;
import com.odde.doughnut.entities.User;
import com.odde.doughnut.entities.repositories.NotebookGroupRepository;
import com.odde.doughnut.exceptions.UnexpectedNoAccessRightException;
import com.odde.doughnut.factoryServices.EntityPersister;
import com.odde.doughnut.services.AuthorizationService;
import com.odde.doughnut.services.NotebookGroupService;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/subscriptions")
class SubscriptionController {
  private final EntityPersister entityPersister;
  private final AuthorizationService authorizationService;
  private final NotebookGroupRepository notebookGroupRepository;
  private final NotebookGroupService notebookGroupService;

  public SubscriptionController(
      EntityPersister entityPersister,
      AuthorizationService authorizationService,
      NotebookGroupRepository notebookGroupRepository,
      NotebookGroupService notebookGroupService) {
    this.entityPersister = entityPersister;
    this.authorizationService = authorizationService;
    this.notebookGroupRepository = notebookGroupRepository;
    this.notebookGroupService = notebookGroupService;
  }

  @PostMapping("/notebooks/{notebook}/subscribe")
  @Transactional
  public @Valid Subscription createSubscription(
      @PathVariable(name = "notebook") @Schema(type = "integer") Notebook notebook,
      @Valid @RequestBody SubscriptionDTO subscriptionDTO)
      throws UnexpectedNoAccessRightException {
    authorizationService.assertReadAuthorization(notebook);
    Subscription subscription = new Subscription();
    subscription.setNotebook(notebook);
    subscription.setFromDTO(subscriptionDTO);
    subscription.setUser(authorizationService.getCurrentUser());
    entityPersister.save(subscription);
    return subscription;
  }

  @PostMapping("/{subscription}")
  @Transactional
  public @Valid Subscription updateSubscription(
      @PathVariable(name = "subscription") @Schema(type = "integer") Subscription subscription,
      @Valid @RequestBody SubscriptionDTO subscriptionDTO)
      throws UnexpectedNoAccessRightException {
    authorizationService.assertReadAuthorization(subscription.getNotebook());
    subscription.setFromDTO(subscriptionDTO);
    entityPersister.save(subscription);
    return subscription;
  }

  @PostMapping("/{subscription}/delete")
  @Transactional
  public List<Integer> destroySubscription(
      @PathVariable(name = "subscription") @Schema(type = "integer") Subscription subscription)
      throws UnexpectedNoAccessRightException {
    authorizationService.assertAuthorization(subscription);
    entityPersister.remove(subscription);
    return List.of(1);
  }

  @PatchMapping("/{subscription}/notebook-group")
  @Transactional
  public Subscription updateSubscriptionGroup(
      @PathVariable(name = "subscription") @Schema(type = "integer") Subscription subscription,
      @RequestBody UpdateNotebookGroupRequest request)
      throws UnexpectedNoAccessRightException {
    authorizationService.assertAuthorization(subscription);
    User user = authorizationService.getCurrentUser();
    if (request.getNotebookGroupId() == null) {
      notebookGroupService.clearSubscriptionGroup(user, subscription);
    } else {
      NotebookGroup group =
          notebookGroupRepository
              .findById(request.getNotebookGroupId())
              .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
      notebookGroupService.assignSubscriptionToGroup(user, subscription, group);
    }
    return subscription;
  }
}

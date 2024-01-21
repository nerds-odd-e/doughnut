package com.odde.doughnut.controllers;

import com.odde.doughnut.entities.Notebook;
import com.odde.doughnut.entities.Subscription;
import com.odde.doughnut.exceptions.UnexpectedNoAccessRightException;
import com.odde.doughnut.factoryServices.ModelFactoryService;
import com.odde.doughnut.models.UserModel;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/subscriptions")
class RestSubscriptionController {
  private final ModelFactoryService modelFactoryService;
  private final UserModel currentUser;

  public RestSubscriptionController(
      ModelFactoryService modelFactoryService, UserModel currentUser) {
    this.modelFactoryService = modelFactoryService;
    this.currentUser = currentUser;
  }

  @PostMapping("/notebooks/{notebook}/subscribe")
  @Transactional
  public @Valid Subscription createSubscription(
      @PathVariable(name = "notebook") Notebook notebook, @Valid Subscription subscription)
      throws UnexpectedNoAccessRightException {
    currentUser.assertReadAuthorization(notebook);
    subscription.setNotebook(notebook);
    subscription.setUser(currentUser.getEntity());
    modelFactoryService.createRecord(subscription);
    return subscription;
  }

  @PostMapping("/{subscription}")
  @Transactional
  public @Valid Subscription update(@Valid Subscription subscription) {
    modelFactoryService.updateRecord(subscription);
    return subscription;
  }

  @PostMapping("/{subscription}/delete")
  @Transactional
  public List<Integer> destroySubscription(@Valid Subscription subscription)
      throws UnexpectedNoAccessRightException {
    currentUser.assertAuthorization(subscription);
    modelFactoryService.entityManager.remove(subscription);
    return List.of(1);
  }
}

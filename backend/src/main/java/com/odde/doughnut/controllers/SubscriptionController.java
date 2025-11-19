package com.odde.doughnut.controllers;

import com.odde.doughnut.controllers.dto.SubscriptionDTO;
import com.odde.doughnut.entities.Notebook;
import com.odde.doughnut.entities.Subscription;
import com.odde.doughnut.exceptions.UnexpectedNoAccessRightException;
import com.odde.doughnut.factoryServices.ModelFactoryService;
import com.odde.doughnut.models.UserModel;
import com.odde.doughnut.services.AuthorizationService;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/subscriptions")
class SubscriptionController {
  private final ModelFactoryService modelFactoryService;
  private final UserModel currentUser;
  private final AuthorizationService authorizationService;

  public SubscriptionController(
      ModelFactoryService modelFactoryService,
      UserModel currentUser,
      AuthorizationService authorizationService) {
    this.modelFactoryService = modelFactoryService;
    this.currentUser = currentUser;
    this.authorizationService = authorizationService;
  }

  @PostMapping("/notebooks/{notebook}/subscribe")
  @Transactional
  public @Valid Subscription createSubscription(
      @PathVariable(name = "notebook") @Schema(type = "integer") Notebook notebook,
      @Valid @RequestBody SubscriptionDTO subscriptionDTO)
      throws UnexpectedNoAccessRightException {
    authorizationService.assertReadAuthorization(currentUser.getEntity(), notebook);
    Subscription subscription = new Subscription();
    subscription.setNotebook(notebook);
    subscription.setFromDTO(subscriptionDTO);
    subscription.setUser(currentUser.getEntity());
    modelFactoryService.save(subscription);
    return subscription;
  }

  @PostMapping("/{subscription}")
  @Transactional
  public @Valid Subscription updateSubscription(
      @PathVariable(name = "subscription") @Schema(type = "integer") Subscription subscription,
      @Valid @RequestBody SubscriptionDTO subscriptionDTO)
      throws UnexpectedNoAccessRightException {
    authorizationService.assertReadAuthorization(
        currentUser.getEntity(), subscription.getNotebook());
    subscription.setFromDTO(subscriptionDTO);
    modelFactoryService.save(subscription);
    return subscription;
  }

  @PostMapping("/{subscription}/delete")
  @Transactional
  public List<Integer> destroySubscription(
      @PathVariable(name = "subscription") @Schema(type = "integer") Subscription subscription)
      throws UnexpectedNoAccessRightException {
    authorizationService.assertAuthorization(currentUser.getEntity(), subscription);
    modelFactoryService.remove(subscription);
    return List.of(1);
  }
}

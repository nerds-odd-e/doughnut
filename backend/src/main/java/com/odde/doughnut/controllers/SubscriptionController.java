package com.odde.doughnut.controllers;

import com.odde.doughnut.controllers.dto.SubscriptionDTO;
import com.odde.doughnut.entities.Notebook;
import com.odde.doughnut.entities.Subscription;
import com.odde.doughnut.exceptions.UnexpectedNoAccessRightException;
import com.odde.doughnut.factoryServices.EntityPersister;
import com.odde.doughnut.services.AuthorizationService;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/subscriptions")
class SubscriptionController {
  private final EntityPersister entityPersister;
  private final AuthorizationService authorizationService;

  public SubscriptionController(
      EntityPersister entityPersister, AuthorizationService authorizationService) {
    this.entityPersister = entityPersister;
    this.authorizationService = authorizationService;
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
}

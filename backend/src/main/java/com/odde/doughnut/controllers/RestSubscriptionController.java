
package com.odde.doughnut.controllers;

import com.odde.doughnut.controllers.currentUser.CurrentUserFetcher;
import com.odde.doughnut.entities.Notebook;
import com.odde.doughnut.entities.Subscription;
import com.odde.doughnut.exceptions.NoAccessRightException;
import com.odde.doughnut.factoryServices.ModelFactoryService;
import com.odde.doughnut.models.BazaarModel;
import com.odde.doughnut.models.UserModel;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

@RestController
@RequestMapping("/api/subscriptions")
class RestSubscriptionController {
  private final ModelFactoryService modelFactoryService;
  private final CurrentUserFetcher currentUserFetcher;

  public RestSubscriptionController(ModelFactoryService modelFactoryService, CurrentUserFetcher currentUserFetcher) {
    this.modelFactoryService = modelFactoryService;
    this.currentUserFetcher = currentUserFetcher;
  }

  @GetMapping("")
  public RestNotebookController.NotebooksViewedByUser bazaar() {
    BazaarModel bazaar = modelFactoryService.toBazaarModel();
    RestNotebookController.NotebooksViewedByUser notebooksViewedByUser = new RestNotebookController.NotebooksViewedByUser();
    notebooksViewedByUser.notebooks = bazaar.getAllNotebooks();
    return notebooksViewedByUser;
  }

  @PostMapping("/notebooks/{notebook}/subscribe")
  @Transactional
  public @Valid Subscription createSubscription(@PathVariable(name = "notebook") Notebook notebook, @Valid Subscription subscription) throws NoAccessRightException {
    final UserModel userModel = currentUserFetcher.getUser();
    userModel.getAuthorization().assertReadAuthorization(notebook);
    subscription.setNotebook(notebook);
    subscription.setUser(userModel.getEntity());
    modelFactoryService.entityManager.persist(subscription);
    return subscription;
  }

  @PostMapping("/{subscription}")
  @Transactional
  public @Valid Subscription update(@Valid Subscription subscription) {
    modelFactoryService.entityManager.persist(subscription);
    return subscription;
  }

}

package com.odde.doughnut.controllers;

import com.odde.doughnut.controllers.currentUser.CurrentUserFetcher;
import com.odde.doughnut.entities.json.NotebooksViewedByUser;
import com.odde.doughnut.factoryServices.ModelFactoryService;
import com.odde.doughnut.models.BazaarModel;
import com.odde.doughnut.models.JsonViewer;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/bazaar")
class RestBazaarController {
  private final ModelFactoryService modelFactoryService;
  private final CurrentUserFetcher currentUserFetcher;

  public RestBazaarController(
      ModelFactoryService modelFactoryService, CurrentUserFetcher currentUserFetcher) {
    this.modelFactoryService = modelFactoryService;
    this.currentUserFetcher = currentUserFetcher;
  }

  @GetMapping("")
  public NotebooksViewedByUser bazaar() {
    BazaarModel bazaar = modelFactoryService.toBazaarModel();
    return new JsonViewer(currentUserFetcher.getUserEntity())
        .jsonNotebooksViewedByUser(bazaar.getAllNotebooks());
  }
}

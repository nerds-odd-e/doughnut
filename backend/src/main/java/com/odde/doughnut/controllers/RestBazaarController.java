package com.odde.doughnut.controllers;

import com.odde.doughnut.controllers.json.NotebooksViewedByUser;
import com.odde.doughnut.factoryServices.ModelFactoryService;
import com.odde.doughnut.models.BazaarModel;
import com.odde.doughnut.models.JsonViewer;
import com.odde.doughnut.models.UserModel;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/bazaar")
class RestBazaarController {
  private final ModelFactoryService modelFactoryService;
  private UserModel currentUser;

  public RestBazaarController(ModelFactoryService modelFactoryService, UserModel currentUser) {
    this.modelFactoryService = modelFactoryService;
    this.currentUser = currentUser;
  }

  @GetMapping("")
  public NotebooksViewedByUser bazaar() {
    BazaarModel bazaar = modelFactoryService.toBazaarModel();
    return new JsonViewer(currentUser.getEntity())
        .jsonNotebooksViewedByUser(bazaar.getAllNotebooks());
  }
}

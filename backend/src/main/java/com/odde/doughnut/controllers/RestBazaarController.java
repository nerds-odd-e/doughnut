package com.odde.doughnut.controllers;

import com.odde.doughnut.controllers.dto.NotebooksViewedByUser;
import com.odde.doughnut.entities.Notebook;
import com.odde.doughnut.exceptions.UnexpectedNoAccessRightException;
import com.odde.doughnut.factoryServices.ModelFactoryService;
import com.odde.doughnut.models.BazaarModel;
import com.odde.doughnut.models.JsonViewer;
import com.odde.doughnut.models.UserModel;
import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

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

  @PostMapping("/{notebook}/remove")
  @Transactional
  public NotebooksViewedByUser removeFromBazaar(
      @PathVariable("notebook") @Schema(type = "integer") Notebook notebook)
      throws UnexpectedNoAccessRightException {
    if (!currentUser.isAdmin()) {
      currentUser.assertAuthorization(notebook);
    }

    BazaarModel bazaar = modelFactoryService.toBazaarModel();
    bazaar.removeFromBazaar(notebook);
    return bazaar();
  }
}

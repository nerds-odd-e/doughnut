package com.odde.doughnut.controllers;

import com.odde.doughnut.entities.BazaarNotebook;
import com.odde.doughnut.exceptions.UnexpectedNoAccessRightException;
import com.odde.doughnut.factoryServices.ModelFactoryService;
import com.odde.doughnut.models.BazaarModel;
import com.odde.doughnut.models.UserModel;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/bazaar")
class BazaarController {
  private final ModelFactoryService modelFactoryService;
  private UserModel currentUser;

  public BazaarController(ModelFactoryService modelFactoryService, UserModel currentUser) {
    this.modelFactoryService = modelFactoryService;
    this.currentUser = currentUser;
  }

  @GetMapping("")
  public List<BazaarNotebook> bazaar() {
    BazaarModel bazaar = modelFactoryService.toBazaarModel();
    return bazaar.getAllBazaarNotebooks();
  }

  @PostMapping("/{bazaarNotebook}/remove")
  @Transactional
  public List<BazaarNotebook> removeFromBazaar(
      @PathVariable("bazaarNotebook") @Schema(type = "integer") BazaarNotebook bazaarNotebook)
      throws UnexpectedNoAccessRightException {
    currentUser.assertAuthorization(bazaarNotebook);

    BazaarModel bazaar = modelFactoryService.toBazaarModel();
    bazaar.removeFromBazaar(bazaarNotebook);
    return bazaar();
  }
}

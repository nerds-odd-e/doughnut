package com.odde.doughnut.controllers;

import com.odde.doughnut.entities.BazaarNotebook;
import com.odde.doughnut.exceptions.UnexpectedNoAccessRightException;
import com.odde.doughnut.services.AuthorizationService;
import com.odde.doughnut.services.BazaarService;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/bazaar")
class BazaarController {
  private final BazaarService bazaarService;
  private final AuthorizationService authorizationService;

  @Autowired
  public BazaarController(BazaarService bazaarService, AuthorizationService authorizationService) {
    this.bazaarService = bazaarService;
    this.authorizationService = authorizationService;
  }

  @GetMapping("")
  public List<BazaarNotebook> bazaar() {
    return bazaarService.getAllBazaarNotebooks();
  }

  @PostMapping("/{bazaarNotebook}/remove")
  @Transactional
  public List<BazaarNotebook> removeFromBazaar(
      @PathVariable("bazaarNotebook") @Schema(type = "integer") BazaarNotebook bazaarNotebook)
      throws UnexpectedNoAccessRightException {
    authorizationService.assertAuthorization(bazaarNotebook);

    bazaarService.removeFromBazaar(bazaarNotebook);
    return bazaar();
  }
}

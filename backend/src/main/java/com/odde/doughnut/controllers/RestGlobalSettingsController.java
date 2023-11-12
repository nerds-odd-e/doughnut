package com.odde.doughnut.controllers;

import com.odde.doughnut.controllers.json.*;
import com.odde.doughnut.factoryServices.ModelFactoryService;
import com.odde.doughnut.models.UserModel;
import com.odde.doughnut.services.GlobalSettingsService;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.annotation.SessionScope;

@RestController
@SessionScope
@RequestMapping("/api/settings")
public class RestGlobalSettingsController {

  private final ModelFactoryService modelFactoryService;
  private final GlobalSettingsService globalSettingsService;
  private UserModel currentUser;

  public RestGlobalSettingsController(
      ModelFactoryService modelFactoryService, UserModel currentUser) {
    this.modelFactoryService = modelFactoryService;
    this.currentUser = currentUser;
    this.globalSettingsService = new GlobalSettingsService(modelFactoryService);
  }

  @GetMapping("/current-model-version")
  public CurrentModelVersionResponse getCurrentModelVersions() {
    return globalSettingsService.getCurrentModelVersions();
  }

  @PostMapping("/current-model-version")
  public CurrentModelVersionResponse setCurrentModelVersions(
      @RequestBody CurrentModelVersionResponse models) {
    return globalSettingsService.setCurrentModelVersions(models);
  }
}

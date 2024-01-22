package com.odde.doughnut.controllers;

import com.odde.doughnut.controllers.json.*;
import com.odde.doughnut.exceptions.UnexpectedNoAccessRightException;
import com.odde.doughnut.factoryServices.ModelFactoryService;
import com.odde.doughnut.models.UserModel;
import com.odde.doughnut.services.GlobalSettingsService;
import com.odde.doughnut.testability.TestabilitySettings;
import jakarta.annotation.Resource;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.annotation.SessionScope;

@RestController
@SessionScope
@RequestMapping("/api/settings")
public class RestGlobalSettingsController {

  private final ModelFactoryService modelFactoryService;
  private final GlobalSettingsService globalSettingsService;

  @Resource(name = "testabilitySettings")
  private final TestabilitySettings testabilitySettings;

  private UserModel currentUser;

  public RestGlobalSettingsController(
      ModelFactoryService modelFactoryService,
      UserModel currentUser,
      TestabilitySettings testabilitySettings) {
    this.modelFactoryService = modelFactoryService;
    this.currentUser = currentUser;
    this.globalSettingsService = new GlobalSettingsService(modelFactoryService);
    this.testabilitySettings = testabilitySettings;
  }

  @GetMapping("/current-model-version")
  public GlobalAiModelSettings getCurrentModelVersions() {
    return globalSettingsService.getCurrentModelVersions();
  }

  @PostMapping("/current-model-version")
  @Transactional
  public GlobalAiModelSettings setCurrentModelVersions(@RequestBody GlobalAiModelSettings models)
      throws UnexpectedNoAccessRightException {
    currentUser.assertAdminAuthorization();
    return globalSettingsService.setCurrentModelVersions(
        models, testabilitySettings.getCurrentUTCTimestamp());
  }
}

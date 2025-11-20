package com.odde.doughnut.controllers;

import com.odde.doughnut.controllers.dto.*;
import com.odde.doughnut.exceptions.UnexpectedNoAccessRightException;
import com.odde.doughnut.services.AuthorizationService;
import com.odde.doughnut.services.GlobalSettingsService;
import com.odde.doughnut.testability.TestabilitySettings;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.annotation.SessionScope;

@RestController
@SessionScope
@RequestMapping("/api/settings")
public class GlobalSettingsController {

  private final GlobalSettingsService globalSettingsService;

  @Resource(name = "testabilitySettings")
  private final TestabilitySettings testabilitySettings;

  private final AuthorizationService authorizationService;

  @Autowired
  public GlobalSettingsController(
      GlobalSettingsService globalSettingsService,
      TestabilitySettings testabilitySettings,
      AuthorizationService authorizationService) {
    this.globalSettingsService = globalSettingsService;
    this.testabilitySettings = testabilitySettings;
    this.authorizationService = authorizationService;
  }

  @GetMapping("/current-model-version")
  public GlobalAiModelSettings getCurrentModelVersions() {
    return globalSettingsService.getCurrentModelVersions();
  }

  @PostMapping("/current-model-version")
  @Transactional
  public GlobalAiModelSettings setCurrentModelVersions(@RequestBody GlobalAiModelSettings models)
      throws UnexpectedNoAccessRightException {
    authorizationService.assertAdminAuthorization();
    return globalSettingsService.setCurrentModelVersions(
        models, testabilitySettings.getCurrentUTCTimestamp());
  }
}

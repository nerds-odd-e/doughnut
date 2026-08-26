package com.odde.donut.controllers;

import com.odde.donut.controllers.dto.*;
import com.odde.donut.exceptions.UnexpectedNoAccessRightException;
import com.odde.donut.services.AuthorizationService;
import com.odde.donut.services.GlobalSettingsService;
import com.odde.donut.testability.TestabilitySettings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.annotation.SessionScope;

@RestController
@SessionScope
@RequestMapping("/api/settings")
public class GlobalSettingsController {

  private final GlobalSettingsService globalSettingsService;

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

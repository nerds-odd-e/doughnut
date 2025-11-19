package com.odde.doughnut.controllers;

import com.odde.doughnut.exceptions.UnexpectedNoAccessRightException;
import com.odde.doughnut.models.UserModel;
import com.odde.doughnut.services.AuthorizationService;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
class HealthCheckController {
  @Autowired private Environment environment;

  @Autowired private UserModel currentUser;

  @Autowired private AuthorizationService authorizationService;

  @GetMapping("/healthcheck")
  public String ping() {
    return "OK. Active Profile: " + String.join(", ", environment.getActiveProfiles());
  }

  @GetMapping("/data_upgrade")
  @Transactional(timeout = 200)
  public List dataUpgrade() throws UnexpectedNoAccessRightException {
    authorizationService.assertAdminAuthorization(currentUser.getEntity());
    return List.of();
  }
}

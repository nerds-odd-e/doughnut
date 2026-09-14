package com.odde.donut.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.info.BuildProperties;
import org.springframework.core.env.Environment;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
class HealthCheckController {
  @Autowired private Environment environment;

  @Autowired private BuildProperties buildProperties;

  @GetMapping("/healthcheck")
  public String ping() {
    return "OK. Active Profile: "
        + String.join(", ", environment.getActiveProfiles())
        + ". Commit: "
        + buildProperties.get("commit");
  }
}

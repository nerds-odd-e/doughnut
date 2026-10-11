package com.odde.donut.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.availability.ApplicationAvailability;
import org.springframework.boot.availability.ReadinessState;
import org.springframework.boot.info.BuildProperties;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
class HealthCheckController {
  @Autowired private Environment environment;

  @Autowired private BuildProperties buildProperties;

  @Autowired private ApplicationAvailability availability;

  @GetMapping("/healthcheck")
  public ResponseEntity<String> ping() {
    if (availability.getReadinessState() != ReadinessState.ACCEPTING_TRAFFIC) {
      return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body("Starting");
    }
    return ResponseEntity.ok(
        "OK. Active Profile: "
            + String.join(", ", environment.getActiveProfiles())
            + ". Commit: "
            + buildProperties.get("commit"));
  }
}


package com.odde.doughnut;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping("/api")
class HealthCheckController {
  @Autowired
  private Environment environment;
  @GetMapping("/healthcheck")
  public String ping() {
    return "OK. Active Profile: " + String.join(", ", environment.getActiveProfiles());
  }
}


package com.odde.doughnut;

import java.net.URI;
import java.net.URISyntaxException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.annotation.GetMapping;

@RestController
@RequestMapping("/api")
class HealthCheckController {
  @Autowired
  private Environment environment;
  @GetMapping("/healthcheck")
  public String ping() {
    return "OK. " + environment.getActiveProfiles();
  }
}

package com.odde.doughnut.controllers;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping
public class InstallController {

  @GetMapping("/install")
  public ResponseEntity<String> install(
      @RequestParam(name = "win32", required = false, defaultValue = "false") boolean win32)
      throws IOException {
    String resourceName = win32 ? "install.ps1" : "install.sh";
    ClassPathResource resource = new ClassPathResource(resourceName);
    String content = new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);

    MediaType mediaType = win32 ? MediaType.parseMediaType("text/plain") : MediaType.TEXT_PLAIN;
    return ResponseEntity.ok().header(HttpHeaders.CONTENT_TYPE, mediaType.toString()).body(content);
  }
}

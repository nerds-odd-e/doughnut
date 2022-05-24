package com.odde.doughnut.controllers;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class RestWikidataController {
  @GetMapping("/wikidata/Q123")
  public String fetchWikidata() {
    return "OK";
  }
}


package com.odde.doughnut.controllers;

import com.odde.doughnut.controllers.currentUser.CurrentUserFetcher;
import com.odde.doughnut.entities.Link;
import com.odde.doughnut.entities.User;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/")
class RestStaticInfoController {
    private final CurrentUserFetcher currentUserFetcher;

  public RestStaticInfoController(CurrentUserFetcher currentUserFetcher) {
      this.currentUserFetcher = currentUserFetcher;
  }

}

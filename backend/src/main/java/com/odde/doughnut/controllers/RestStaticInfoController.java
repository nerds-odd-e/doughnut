
package com.odde.doughnut.controllers;

import com.odde.doughnut.controllers.currentUser.CurrentUserFetcher;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/")
class RestStaticInfoController {
    private final CurrentUserFetcher currentUserFetcher;

  public RestStaticInfoController(CurrentUserFetcher currentUserFetcher) {
      this.currentUserFetcher = currentUserFetcher;
  }

}

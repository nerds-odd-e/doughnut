package com.odde.doughnut.controllers;

import com.odde.doughnut.controllers.currentUser.CurrentUserFetcher;
import com.odde.doughnut.entities.json.CurrentUserInfo;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/user")
record RestCurrentUserInfoController(CurrentUserFetcher currentUserFetcher) {
  @GetMapping("/current-user-info")
  public CurrentUserInfo currentUserInfo() {
    CurrentUserInfo currentUserInfo = new CurrentUserInfo();
    currentUserInfo.user = currentUserFetcher.getUser().getEntity();
    currentUserInfo.externalIdentifier = currentUserFetcher.getExternalIdentifier();
    currentUserInfo.role = currentUserFetcher.getUser().getRole();
    return currentUserInfo;
  }
}

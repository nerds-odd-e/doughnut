
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

  static class StaticInfo {
    public List<Map<String, String>> linkTypeOptions = new ArrayList<>();
    public User user;
  }

  @GetMapping("/static-info")
  public StaticInfo staticInfo() {
      StaticInfo staticInfo = new StaticInfo();
    staticInfo.linkTypeOptions = Arrays.stream(Link.LinkType.values())
            .map( (linkType)->new HashMap<String, String>(){{
                        put("value", linkType.id.toString());
                        put("label", linkType.label);
                    }})
            .collect(Collectors.toList());
    staticInfo.user = currentUserFetcher.getUser().getEntity();
    return staticInfo;
  }
}

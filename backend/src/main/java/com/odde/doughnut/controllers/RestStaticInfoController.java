
package com.odde.doughnut.controllers;

import com.odde.doughnut.entities.Link;
import com.odde.doughnut.entities.ReviewPoint;
import com.odde.doughnut.entities.json.LinkViewedByUser;
import com.odde.doughnut.exceptions.NoAccessRightException;
import com.odde.doughnut.models.UserModel;
import lombok.Getter;
import lombok.Setter;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/")
class RestStaticInfoController {

  public RestStaticInfoController() {
  }

  static class StaticInfo {
    public List<Map<String, String>> linkTypeOptions = new ArrayList<>();
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
    return staticInfo;
  }
}

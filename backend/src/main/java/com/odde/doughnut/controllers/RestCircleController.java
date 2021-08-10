
package com.odde.doughnut.controllers;

import com.odde.doughnut.controllers.currentUser.CurrentUserFetcher;
import com.odde.doughnut.entities.*;
import com.odde.doughnut.entities.json.RedirectToNoteResponse;
import com.odde.doughnut.exceptions.NoAccessRightException;
import com.odde.doughnut.factoryServices.ModelFactoryService;
import com.odde.doughnut.models.UserModel;
import lombok.Getter;
import lombok.Setter;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/circles")
class RestCircleController {
  private final ModelFactoryService modelFactoryService;
  private final CurrentUserFetcher currentUserFetcher;

  public RestCircleController(ModelFactoryService modelFactoryService, CurrentUserFetcher currentUserFetcher) {
    this.modelFactoryService = modelFactoryService;
    this.currentUserFetcher = currentUserFetcher;
  }

  static class UserForOtherUserView {
    @Setter @Getter String name;

    public static List<UserForOtherUserView> fromList(List<User> users) {
      return users.stream().map(u->{
        UserForOtherUserView ufv = new UserForOtherUserView();
        ufv.setName(u.getName());
        return ufv;
      }).collect(Collectors.toUnmodifiableList());
    }
  }

  static class CircleForUserView {
    @Setter @Getter
    Integer id;
    @Setter @Getter
    String name;
    @Setter @Getter
    String invitationCode;
    @Setter @Getter
    List<Notebook> notebooks;
    @Setter @Getter
    List<UserForOtherUserView> members;
  }

  @GetMapping("/{circle}")
  public CircleForUserView showCircle(@PathVariable("circle") Circle circle) throws NoAccessRightException {
    currentUserFetcher.getUser().getAuthorization().assertAuthorization(circle);
    CircleForUserView circleForUserView = new CircleForUserView();
    circleForUserView.setId(circle.getId());
    circleForUserView.setName(circle.getName());
    circleForUserView.setInvitationCode(circle.getInvitationCode());
    circleForUserView.setNotebooks(circle.getOwnership().getNotebooks());
    circleForUserView.setMembers(UserForOtherUserView.fromList(circle.getMembers()));
    return circleForUserView;
  }

}

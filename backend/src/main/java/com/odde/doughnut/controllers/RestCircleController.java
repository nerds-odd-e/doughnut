
package com.odde.doughnut.controllers;

import com.odde.doughnut.controllers.currentUser.CurrentUserFetcher;
import com.odde.doughnut.entities.*;
import com.odde.doughnut.entities.json.RedirectToNoteResponse;
import com.odde.doughnut.exceptions.NoAccessRightException;
import com.odde.doughnut.factoryServices.ModelFactoryService;
import com.odde.doughnut.models.CircleModel;
import com.odde.doughnut.models.UserModel;
import lombok.Getter;
import lombok.Setter;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindException;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.io.IOException;
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

  @GetMapping("")
  public List<Circle> index(Model model) {
    UserModel user = currentUserFetcher.getUser();
    return user.getEntity().getCircles();
  }

  @PostMapping("")
  public Circle createCircle(@Valid Circle circle) {
    UserModel userModel = currentUserFetcher.getUser();
    CircleModel circleModel = modelFactoryService.toCircleModel(circle);
    circleModel.joinAndSave(userModel);
    return circle;
  }

  @PostMapping("/join")
  @Transactional
  public Circle joinCircle(@Valid CircleController.CircleJoiningByInvitation circleJoiningByInvitation) throws BindException {
    CircleModel circleModel = modelFactoryService.findCircleByInvitationCode(circleJoiningByInvitation.getInvitationCode());
    if (circleModel == null) {
      BindingResult bindingResult = new BeanPropertyBindingResult(circleJoiningByInvitation, "circle");
      bindingResult.rejectValue("invitationCode", "error.error", "Does not match any circle");

      throw new BindException(bindingResult);
    }
    UserModel userModel = currentUserFetcher.getUser();
    if (userModel.getEntity().inCircle(circleModel.getEntity())) {
      BindingResult bindingResult = new BeanPropertyBindingResult(circleJoiningByInvitation, "circle");
      bindingResult.rejectValue("invitationCode", "error.error", "You are already in this circle");
      throw new BindException(bindingResult);
    }
    circleModel.joinAndSave(userModel);
    return circleModel.getEntity();
  }

  @PostMapping({"/{circle}/notebooks"})
  public RedirectToNoteResponse createNotebook(Circle circle, @Valid @ModelAttribute NoteContent noteContent) throws IOException, NoAccessRightException {
    UserModel user = currentUserFetcher.getUser();
    user.getAuthorization().assertLoggedIn();
    currentUserFetcher.getUser().getAuthorization().assertAuthorization(circle);
    Note note = circle.getOwnership().createNotebook(user.getEntity(), noteContent);
    modelFactoryService.noteRepository.save(note);
    return new RedirectToNoteResponse(note.getId());
  }

}

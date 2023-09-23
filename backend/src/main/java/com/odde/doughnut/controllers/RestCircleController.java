package com.odde.doughnut.controllers;

import com.odde.doughnut.entities.Circle;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.TextContent;
import com.odde.doughnut.entities.User;
import com.odde.doughnut.controllers.json.CircleForUserView;
import com.odde.doughnut.controllers.json.CircleJoiningByInvitation;
import com.odde.doughnut.controllers.json.RedirectToNoteResponse;
import com.odde.doughnut.exceptions.UnexpectedNoAccessRightException;
import com.odde.doughnut.factoryServices.ModelFactoryService;
import com.odde.doughnut.models.CircleModel;
import com.odde.doughnut.models.JsonViewer;
import com.odde.doughnut.models.UserModel;
import com.odde.doughnut.testability.TestabilitySettings;
import java.util.List;
import javax.annotation.Resource;
import javax.validation.Valid;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindException;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/circles")
class RestCircleController {
  private final ModelFactoryService modelFactoryService;

  @Resource(name = "testabilitySettings")
  private final TestabilitySettings testabilitySettings;

  private UserModel currentUser;

  public RestCircleController(
      ModelFactoryService modelFactoryService,
      TestabilitySettings testabilitySettings,
      UserModel currentUser) {
    this.modelFactoryService = modelFactoryService;
    this.testabilitySettings = testabilitySettings;
    this.currentUser = currentUser;
  }

  @GetMapping("/{circle}")
  public CircleForUserView showCircle(@PathVariable("circle") Circle circle)
      throws UnexpectedNoAccessRightException {
    currentUser.assertAuthorization(circle);
    JsonViewer jsonViewer = new JsonViewer(currentUser.getEntity());
    return jsonViewer.jsonCircleForUserView(circle);
  }

  @GetMapping("")
  public List<Circle> index() {
    currentUser.assertLoggedIn();
    return currentUser.getEntity().getCircles();
  }

  @PostMapping("")
  public Circle createCircle(@Valid Circle circle) {
    CircleModel circleModel = modelFactoryService.toCircleModel(circle);
    circleModel.joinAndSave(currentUser.getEntity());
    return circle;
  }

  @PostMapping("/join")
  @Transactional
  public Circle joinCircle(@Valid CircleJoiningByInvitation circleJoiningByInvitation)
      throws BindException {
    CircleModel circleModel =
        modelFactoryService.findCircleByInvitationCode(
            circleJoiningByInvitation.getInvitationCode());
    if (circleModel == null) {
      BindingResult bindingResult =
          new BeanPropertyBindingResult(circleJoiningByInvitation, "circle");
      bindingResult.rejectValue("invitationCode", "error.error", "Does not match any circle");

      throw new BindException(bindingResult);
    }
    User user = currentUser.getEntity();
    if (user.inCircle(circleModel.getEntity())) {
      BindingResult bindingResult =
          new BeanPropertyBindingResult(circleJoiningByInvitation, "circle");
      bindingResult.rejectValue("invitationCode", "error.error", "You are already in this circle");
      throw new BindException(bindingResult);
    }
    circleModel.joinAndSave(user);
    return circleModel.getEntity();
  }

  @PostMapping({"/{circle}/notebooks"})
  public RedirectToNoteResponse createNotebook(
      Circle circle, @Valid @ModelAttribute TextContent textContent)
      throws UnexpectedNoAccessRightException {
    currentUser.assertLoggedIn();
    currentUser.assertAuthorization(circle);
    Note note =
        circle
            .getOwnership()
            .createNotebook(
                currentUser.getEntity(), textContent, testabilitySettings.getCurrentUTCTimestamp());
    modelFactoryService.noteRepository.save(note);
    return new RedirectToNoteResponse(note.getId());
  }
}

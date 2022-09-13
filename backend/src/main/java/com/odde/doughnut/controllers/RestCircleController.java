package com.odde.doughnut.controllers;

import com.odde.doughnut.controllers.currentUser.CurrentUserFetcher;
import com.odde.doughnut.entities.Circle;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.TextContent;
import com.odde.doughnut.entities.User;
import com.odde.doughnut.entities.json.CircleForUserView;
import com.odde.doughnut.entities.json.CircleJoiningByInvitation;
import com.odde.doughnut.entities.json.RedirectToNoteResponse;
import com.odde.doughnut.exceptions.NoAccessRightException;
import com.odde.doughnut.factoryServices.ModelFactoryService;
import com.odde.doughnut.models.CircleModel;
import com.odde.doughnut.models.JsonViewer;
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
  private final CurrentUserFetcher currentUserFetcher;

  @Resource(name = "testabilitySettings")
  private final TestabilitySettings testabilitySettings;

  public RestCircleController(
      ModelFactoryService modelFactoryService,
      CurrentUserFetcher currentUserFetcher,
      TestabilitySettings testabilitySettings) {
    this.modelFactoryService = modelFactoryService;
    this.currentUserFetcher = currentUserFetcher;
    this.testabilitySettings = testabilitySettings;
  }

  @GetMapping("/{circle}")
  public CircleForUserView showCircle(@PathVariable("circle") Circle circle)
      throws NoAccessRightException {
    currentUserFetcher.assertAuthorization(circle);
    JsonViewer jsonViewer = new JsonViewer(currentUserFetcher.getUserEntity());
    return jsonViewer.jsonCircleForUserView(circle);
  }

  @GetMapping("")
  public List<Circle> index() {
    currentUserFetcher.assertLoggedIn();
    return currentUserFetcher.getUserEntity().getCircles();
  }

  @PostMapping("")
  public Circle createCircle(@Valid Circle circle) {
    CircleModel circleModel = modelFactoryService.toCircleModel(circle);
    circleModel.joinAndSave(currentUserFetcher.getUserEntity());
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
    User user = currentUserFetcher.getUserEntity();
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
      Circle circle, @Valid @ModelAttribute TextContent textContent) throws NoAccessRightException {
    currentUserFetcher.assertLoggedIn();
    currentUserFetcher.assertAuthorization(circle);
    Note note =
        circle
            .getOwnership()
            .createNotebook(
                currentUserFetcher.getUserEntity(),
                textContent,
                testabilitySettings.getCurrentUTCTimestamp());
    modelFactoryService.noteRepository.save(note);
    return new RedirectToNoteResponse(note.getId());
  }
}

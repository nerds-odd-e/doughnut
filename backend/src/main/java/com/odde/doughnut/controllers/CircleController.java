package com.odde.doughnut.controllers;

import com.odde.doughnut.controllers.dto.CircleForUserView;
import com.odde.doughnut.controllers.dto.CircleJoiningByInvitation;
import com.odde.doughnut.controllers.dto.NoteCreationDTO;
import com.odde.doughnut.controllers.dto.NotebooksViewedByUser;
import com.odde.doughnut.controllers.dto.RedirectToNoteResponse;
import com.odde.doughnut.entities.Circle;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.Notebook;
import com.odde.doughnut.entities.NotebookGroup;
import com.odde.doughnut.entities.User;
import com.odde.doughnut.entities.repositories.NotebookGroupRepository;
import com.odde.doughnut.entities.repositories.NotebookRepository;
import com.odde.doughnut.exceptions.UnexpectedNoAccessRightException;
import com.odde.doughnut.services.AuthorizationService;
import com.odde.doughnut.services.CircleService;
import com.odde.doughnut.services.NotebookCatalogService;
import com.odde.doughnut.services.NotebookService;
import com.odde.doughnut.testability.TestabilitySettings;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindException;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/circles")
class CircleController {
  private final CircleService circleService;
  private final NotebookService notebookService;

  private final TestabilitySettings testabilitySettings;

  private final AuthorizationService authorizationService;
  private final NotebookGroupRepository notebookGroupRepository;
  private final NotebookRepository notebookRepository;
  private final NotebookCatalogService notebookCatalogService;

  public CircleController(
      CircleService circleService,
      NotebookService notebookService,
      TestabilitySettings testabilitySettings,
      AuthorizationService authorizationService,
      NotebookGroupRepository notebookGroupRepository,
      NotebookRepository notebookRepository,
      NotebookCatalogService notebookCatalogService) {
    this.circleService = circleService;
    this.notebookService = notebookService;
    this.testabilitySettings = testabilitySettings;
    this.authorizationService = authorizationService;
    this.notebookGroupRepository = notebookGroupRepository;
    this.notebookRepository = notebookRepository;
    this.notebookCatalogService = notebookCatalogService;
  }

  @GetMapping("/{circle}")
  public CircleForUserView showCircle(
      @PathVariable("circle") @Schema(type = "integer") Circle circle)
      throws UnexpectedNoAccessRightException {
    authorizationService.assertAuthorization(circle);
    var ownership = circle.getOwnership();
    List<NotebookGroup> groups = notebookGroupRepository.findByOwnership_Id(ownership.getId());
    List<Notebook> notebooks =
        notebookRepository.findByOwnership_IdAndDeletedAtIsNull(ownership.getId());
    NotebooksViewedByUser notebooksView =
        notebookCatalogService.buildView(notebooks, groups, List.of());
    return circle.jsonCircleForUserView(notebooksView);
  }

  @GetMapping("")
  public List<Circle> index() {
    authorizationService.assertLoggedIn();
    return authorizationService.getCurrentUser().getCircles();
  }

  @PostMapping("")
  @Transactional
  public Circle createCircle(@Valid @RequestBody Circle circle) {
    circleService.joinAndSave(circle, authorizationService.getCurrentUser());
    return circle;
  }

  @PostMapping("/join")
  @Transactional
  public Circle joinCircle(@Valid @RequestBody CircleJoiningByInvitation circleJoiningByInvitation)
      throws BindException {
    Circle circle =
        circleService.findCircleByInvitationCode(circleJoiningByInvitation.getInvitationCode());
    if (circle == null) {
      BindingResult bindingResult =
          new BeanPropertyBindingResult(circleJoiningByInvitation, "circle");
      bindingResult.rejectValue("invitationCode", "error.error", "Does not match any circle");

      throw new BindException(bindingResult);
    }
    User user = authorizationService.getCurrentUser();
    if (user.inCircle(circle)) {
      BindingResult bindingResult =
          new BeanPropertyBindingResult(circleJoiningByInvitation, "circle");
      bindingResult.rejectValue("invitationCode", "error.error", "You are already in this circle");
      throw new BindException(bindingResult);
    }
    circleService.joinAndSave(circle, user);
    return circle;
  }

  @PostMapping({"/{circle}/notebooks"})
  @Transactional
  public RedirectToNoteResponse createNotebookInCircle(
      @PathVariable @Schema(type = "integer") Circle circle,
      @Valid @RequestBody NoteCreationDTO noteCreation)
      throws UnexpectedNoAccessRightException {
    authorizationService.assertLoggedIn();
    authorizationService.assertAuthorization(circle);
    Note note =
        notebookService.createNotebookForOwnership(
            circle.getOwnership(),
            authorizationService.getCurrentUser(),
            testabilitySettings.getCurrentUTCTimestamp(),
            noteCreation.getNewTitle());
    return new RedirectToNoteResponse(note.getId());
  }
}

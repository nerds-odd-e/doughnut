package com.odde.doughnut.controllers;

import com.odde.doughnut.controllers.json.NoteCreation;
import com.odde.doughnut.controllers.json.NotebooksViewedByUser;
import com.odde.doughnut.controllers.json.RedirectToNoteResponse;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.Notebook;
import com.odde.doughnut.entities.User;
import com.odde.doughnut.exceptions.UnexpectedNoAccessRightException;
import com.odde.doughnut.factoryServices.ModelFactoryService;
import com.odde.doughnut.models.BazaarModel;
import com.odde.doughnut.models.JsonViewer;
import com.odde.doughnut.models.UserModel;
import com.odde.doughnut.testability.TestabilitySettings;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/notebooks")
class RestNotebookController {
  private final ModelFactoryService modelFactoryService;
  private UserModel currentUser;

  @Resource(name = "testabilitySettings")
  private final TestabilitySettings testabilitySettings;

  public RestNotebookController(
      ModelFactoryService modelFactoryService,
      UserModel currentUser,
      TestabilitySettings testabilitySettings) {
    this.modelFactoryService = modelFactoryService;
    this.currentUser = currentUser;
    this.testabilitySettings = testabilitySettings;
  }

  @GetMapping("")
  public NotebooksViewedByUser myNotebooks() {
    currentUser.assertLoggedIn();

    User user = currentUser.getEntity();
    NotebooksViewedByUser notebooksViewedByUser =
        new JsonViewer(user).jsonNotebooksViewedByUser(user.getOwnership().getNotebooks());
    notebooksViewedByUser.subscriptions = user.getSubscriptions();
    return notebooksViewedByUser;
  }

  @PostMapping({"/create"})
  @Transactional
  public RedirectToNoteResponse createNotebook(@Valid @ModelAttribute NoteCreation noteCreation) {
    currentUser.assertLoggedIn();
    User userEntity = currentUser.getEntity();
    Note note =
        userEntity
            .getOwnership()
            .createAndPersistNotebook(
                userEntity, testabilitySettings.getCurrentUTCTimestamp(),
                modelFactoryService, noteCreation.getTopicConstructor());
    return new RedirectToNoteResponse(note.getId());
  }

  @PostMapping(value = "/{notebook}")
  @Transactional
  public Notebook update(@Valid Notebook notebook) throws UnexpectedNoAccessRightException {
    currentUser.assertAuthorization(notebook);
    modelFactoryService.save(notebook);
    return notebook;
  }

  @PostMapping(value = "/{notebook}/share")
  @Transactional
  public Notebook shareNote(@PathVariable("notebook") Notebook notebook)
      throws UnexpectedNoAccessRightException {
    currentUser.assertAuthorization(notebook);
    BazaarModel bazaar = modelFactoryService.toBazaarModel();
    bazaar.shareNote(notebook);
    return notebook;
  }
}

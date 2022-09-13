package com.odde.doughnut.controllers;

import com.odde.doughnut.controllers.currentUser.CurrentUserFetcher;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.Notebook;
import com.odde.doughnut.entities.TextContent;
import com.odde.doughnut.entities.User;
import com.odde.doughnut.entities.json.NotebooksViewedByUser;
import com.odde.doughnut.entities.json.RedirectToNoteResponse;
import com.odde.doughnut.exceptions.NoAccessRightException;
import com.odde.doughnut.factoryServices.ModelFactoryService;
import com.odde.doughnut.models.BazaarModel;
import com.odde.doughnut.models.JsonViewer;
import com.odde.doughnut.models.UserModel;
import com.odde.doughnut.testability.TestabilitySettings;
import javax.annotation.Resource;
import javax.validation.Valid;
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
  private final CurrentUserFetcher currentUserFetcher;

  @Resource(name = "testabilitySettings")
  private final TestabilitySettings testabilitySettings;

  public RestNotebookController(
      ModelFactoryService modelFactoryService,
      CurrentUserFetcher currentUserFetcher,
      TestabilitySettings testabilitySettings) {
    this.modelFactoryService = modelFactoryService;
    this.currentUserFetcher = currentUserFetcher;
    this.testabilitySettings = testabilitySettings;
  }

  @GetMapping("")
  public NotebooksViewedByUser myNotebooks() {
    currentUserFetcher.assertLoggedIn();

    UserModel user = currentUserFetcher.getUser();
    NotebooksViewedByUser notebooksViewedByUser =
        new JsonViewer(user.getEntity())
            .jsonNotebooksViewedByUser(user.getEntity().getOwnership().getNotebooks());
    notebooksViewedByUser.subscriptions = user.getEntity().getSubscriptions();
    return notebooksViewedByUser;
  }

  @PostMapping({"/create"})
  public RedirectToNoteResponse createNotebook(@Valid @ModelAttribute TextContent textContent) {
    currentUserFetcher.assertLoggedIn();
    User userEntity = currentUserFetcher.getUser().getEntity();
    Note note =
        userEntity
            .getOwnership()
            .createNotebook(userEntity, textContent, testabilitySettings.getCurrentUTCTimestamp());
    modelFactoryService.noteRepository.save(note);
    return new RedirectToNoteResponse(note.getId());
  }

  @PostMapping(value = "/{notebook}")
  @Transactional
  public Notebook update(@Valid Notebook notebook) throws NoAccessRightException {
    currentUserFetcher.assertAuthorization(notebook);
    modelFactoryService.notebookRepository.save(notebook);
    return notebook;
  }

  @PostMapping(value = "/{notebook}/share")
  public Notebook shareNote(@PathVariable("notebook") Notebook notebook)
      throws NoAccessRightException {
    currentUserFetcher.assertAuthorization(notebook);
    BazaarModel bazaar = modelFactoryService.toBazaarModel();
    bazaar.shareNote(notebook);
    return notebook;
  }
}

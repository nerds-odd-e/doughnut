
package com.odde.doughnut.controllers;

import com.odde.doughnut.controllers.currentUser.CurrentUserFetcher;
import com.odde.doughnut.entities.*;
import com.odde.doughnut.entities.json.RedirectToNoteResponse;
import com.odde.doughnut.factoryServices.ModelFactoryService;
import com.odde.doughnut.models.UserModel;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/notebooks")
class RestNotebookController {
  private final ModelFactoryService modelFactoryService;
  private final CurrentUserFetcher currentUserFetcher;

  public RestNotebookController(ModelFactoryService modelFactoryService, CurrentUserFetcher currentUserFetcher) {
    this.modelFactoryService = modelFactoryService;
    this.currentUserFetcher = currentUserFetcher;
  }

  static class NotebooksViewedByUser {
    public List<Notebook> notebooks;
    public List<Subscription> subscriptions;
  }

  @GetMapping("")
  public NotebooksViewedByUser myNotebooks() {
    UserModel user = currentUserFetcher.getUser();
    NotebooksViewedByUser notebooksViewedByUser = new NotebooksViewedByUser();
    notebooksViewedByUser.notebooks = user.getEntity().getOwnership().getNotebooks();
    notebooksViewedByUser.subscriptions = user.getEntity().getSubscriptions();
    return notebooksViewedByUser;
  }

  @PostMapping({"/create"})
  public RedirectToNoteResponse createNote(@Valid @ModelAttribute NoteContent noteContent) throws IOException {
    UserModel user = currentUserFetcher.getUser();
    user.getAuthorization().assertLoggedIn();
    final Note note = createHeadNote(user.getEntity(), noteContent);
    modelFactoryService.noteRepository.save(note);
    return new RedirectToNoteResponse(note.getId());
  }

  private Note createHeadNote(User user, NoteContent noteContent) throws IOException {
    final Note note = new Note();
    note.updateNoteContent(noteContent, user);
    note.buildNotebookForHeadNote(user.getOwnership(), user);
    return note;
  }


}


package com.odde.doughnut.controllers;

import com.odde.doughnut.controllers.currentUser.CurrentUserFetcher;
import com.odde.doughnut.entities.*;
import com.odde.doughnut.entities.json.RedirectToNoteResponse;
import com.odde.doughnut.factoryServices.ModelFactoryService;
import com.odde.doughnut.models.UserModel;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.io.IOException;

@RestController
@RequestMapping("/api/notebooks")
class RestNotebookController {
  private final ModelFactoryService modelFactoryService;
  private final CurrentUserFetcher currentUserFetcher;

  public RestNotebookController(ModelFactoryService modelFactoryService, CurrentUserFetcher currentUserFetcher) {
    this.modelFactoryService = modelFactoryService;
    this.currentUserFetcher = currentUserFetcher;
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

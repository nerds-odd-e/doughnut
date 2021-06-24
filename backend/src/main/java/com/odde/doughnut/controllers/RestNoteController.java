
package com.odde.doughnut.controllers;

import com.odde.doughnut.controllers.currentUser.CurrentUserFetcher;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.NoteContent;
import com.odde.doughnut.entities.ReviewPoint;
import com.odde.doughnut.entities.json.NoteViewedByUser;
import com.odde.doughnut.entities.json.RedirectToNoteResponse;
import com.odde.doughnut.exceptions.NoAccessRightException;
import com.odde.doughnut.factoryServices.ModelFactoryService;
import com.odde.doughnut.models.UserModel;
import lombok.Getter;
import lombok.Setter;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.io.IOException;

@RestController
@RequestMapping("/api/notes")
class RestNoteController {
  private final ModelFactoryService modelFactoryService;
  private final CurrentUserFetcher currentUserFetcher;

  public RestNoteController(ModelFactoryService modelFactoryService, CurrentUserFetcher currentUserFetcher) {
    this.modelFactoryService = modelFactoryService;
    this.currentUserFetcher = currentUserFetcher;
  }

  class NoteStatistics {
    @Getter
    @Setter
    private ReviewPoint reviewPoint;
    @Getter
    @Setter
    private Note note;

  }

  @GetMapping("/{note}")
  public NoteViewedByUser show(@PathVariable("note") Note note) throws NoAccessRightException {
    final UserModel user = currentUserFetcher.getUser();
    user.getAuthorization().assertReadAuthorization(note);
    return note.jsonObjectViewedBy(user.getEntity());
  }

  @PostMapping("/{note}")
  public RedirectToNoteResponse updateNote(@PathVariable(name = "note") Note note, @Valid @RequestBody NoteContent noteContent, BindingResult bindingResult) throws NoAccessRightException, IOException {
    final UserModel user = currentUserFetcher.getUser();
    user.getAuthorization().assertAuthorization(note);
    if (bindingResult.hasErrors()) {
      return null;
    }
    note.updateNoteContent(noteContent, user.getEntity());
    modelFactoryService.noteRepository.save(note);
    return new RedirectToNoteResponse(note.getId());
  }

  @GetMapping("/{note}/statistics")
  public NoteStatistics statistics(@PathVariable("note") Note note) throws NoAccessRightException {
    final UserModel user = currentUserFetcher.getUser();
    user.getAuthorization().assertReadAuthorization(note);
    NoteStatistics statistics = new NoteStatistics();
    statistics.setReviewPoint(user.getReviewPointFor(note));
    statistics.setNote(note);
    return statistics;
  }

}

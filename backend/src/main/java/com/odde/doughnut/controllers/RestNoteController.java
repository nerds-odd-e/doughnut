
package com.odde.doughnut.controllers;

import com.odde.doughnut.controllers.currentUser.CurrentUserFetcher;
import com.odde.doughnut.entities.*;
import com.odde.doughnut.entities.json.NoteViewedByUser;
import com.odde.doughnut.entities.json.RedirectToNoteResponse;
import com.odde.doughnut.exceptions.NoAccessRightException;
import com.odde.doughnut.factoryServices.ModelFactoryService;
import com.odde.doughnut.models.UserModel;
import lombok.Getter;
import lombok.Setter;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.io.IOException;
import java.util.List;

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

  static class NoteCreation {
    @Getter @Setter
    private Integer linkTypeToParent;
    @Getter @Setter
    @Valid
    @NotNull
    private NoteContent noteContent = new NoteContent();
  }

  @PostMapping(value="/{parentNote}/create")
  public RedirectToNoteResponse createNote(@PathVariable(name = "parentNote") Note parentNote, @Valid @ModelAttribute NoteCreation noteCreation) throws NoAccessRightException, IOException {
    final UserModel userModel = currentUserFetcher.getUser();
    userModel.getAuthorization().assertAuthorization(parentNote);
    User user = userModel.getEntity();
    Note note = new Note();
    note.updateNoteContent(noteCreation.getNoteContent(), user);
    note.setParentNote(parentNote);
    note.setUser(user);
    modelFactoryService.noteRepository.save(note);
    return new RedirectToNoteResponse(note.getId());
  }


  @GetMapping("/{note}")
  public NoteViewedByUser show(@PathVariable("note") Note note) throws NoAccessRightException {
    final UserModel user = currentUserFetcher.getUser();
    user.getAuthorization().assertReadAuthorization(note);
    return note.jsonObjectViewedBy(user.getEntity());
  }

  @PostMapping(path="/{note}")
  public RedirectToNoteResponse updateNote(@PathVariable(name = "note") Note note, @Valid @ModelAttribute NoteContent noteContent) throws NoAccessRightException, IOException {
    final UserModel user = currentUserFetcher.getUser();
    user.getAuthorization().assertAuthorization(note);
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

  @PostMapping("/{note}/search")
  public List<Note> searchForLinkTarget(@PathVariable("note") Note note, @Valid @RequestBody SearchTerm searchTerm) {
    return currentUserFetcher.getUser().getLinkableNotes(note, searchTerm);
  }

}

package com.odde.doughnut.controllers;

import com.odde.doughnut.controllers.currentUser.CurrentUserFetcher;
import com.odde.doughnut.entities.*;
import com.odde.doughnut.entities.json.*;
import com.odde.doughnut.exceptions.NoAccessRightException;
import com.odde.doughnut.factoryServices.ModelFactoryService;
import com.odde.doughnut.models.NoteViewer;
import com.odde.doughnut.models.SearchTermModel;
import com.odde.doughnut.models.UserModel;
import com.odde.doughnut.testability.TestabilitySettings;
import java.io.IOException;
import java.sql.Timestamp;
import java.util.List;
import javax.annotation.Resource;
import javax.validation.Valid;
import lombok.Getter;
import lombok.Setter;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/notes")
class RestNoteController {
  private final ModelFactoryService modelFactoryService;
  private final CurrentUserFetcher currentUserFetcher;

  @Resource(name = "testabilitySettings")
  private final TestabilitySettings testabilitySettings;

  public RestNoteController(
      ModelFactoryService modelFactoryService,
      CurrentUserFetcher currentUserFetcher,
      TestabilitySettings testabilitySettings) {
    this.modelFactoryService = modelFactoryService;
    this.currentUserFetcher = currentUserFetcher;
    this.testabilitySettings = testabilitySettings;
  }

  @PostMapping(value = "/{note}/createComment")
  @Transactional
  public String createComment(Note note, @RequestBody CommentCreation commentCreation)
      throws NoAccessRightException {
    final UserModel userModel = currentUserFetcher.getUser();
    userModel.getAuthorization().assertAuthorization(note);

    Comment comment = new Comment();
    comment.setNote(note);
    comment.setUser(userModel.getEntity());
    comment.setContent(commentCreation.description);
    modelFactoryService.commentRepository.save(comment);
    return "{}";
  }

  @GetMapping(value = "/{note}/comments")
  public List<Comment> getComments(Note note) throws NoAccessRightException {
    final UserModel userModel = currentUserFetcher.getUser();
    userModel.getAuthorization().assertReadAuthorization(note);

    return modelFactoryService.commentRepository.findAllByNote(note);
  }

  static class NoteStatistics {
    @Getter @Setter private ReviewPoint reviewPoint;
    @Getter @Setter private NoteRealm note;
    @Getter @Setter private Timestamp createdAt;
  }

  @PostMapping(value = "/{parentNote}/create")
  @Transactional
  public NoteRealmWithPosition createNote(
      @PathVariable(name = "parentNote") Note parentNote,
      @Valid @ModelAttribute NoteCreation noteCreation)
      throws NoAccessRightException {
    final UserModel userModel = currentUserFetcher.getUser();
    userModel.getAuthorization().assertAuthorization(parentNote);
    User user = userModel.getEntity();
    Note note =
        Note.createNote(
            user, testabilitySettings.getCurrentUTCTimestamp(), noteCreation.textContent);
    note.setParentNote(parentNote);
    modelFactoryService.noteRepository.save(note);
    Integer linkTypeToParent = noteCreation.getLinkTypeToParent();
    if (linkTypeToParent != null && linkTypeToParent != 0) {
      Link link =
          Link.createLink(
              note,
              parentNote,
              user,
              linkTypeToParent,
              testabilitySettings.getCurrentUTCTimestamp());
      modelFactoryService.linkRepository.save(link);
    }
    return NoteRealmWithPosition.fromNote(note, userModel);
  }

  @GetMapping("/{note}")
  public NotesBulk show(@PathVariable("note") Note note) throws NoAccessRightException {
    final UserModel user = currentUserFetcher.getUser();
    user.getAuthorization().assertReadAuthorization(note);

    return NotesBulk.jsonNoteRealm(note, user);
  }

  @GetMapping("/{note}/overview")
  public NoteRealmWithAllDescendants showOverview(@PathVariable("note") Note note)
      throws NoAccessRightException {
    final UserModel user = currentUserFetcher.getUser();
    user.getAuthorization().assertReadAuthorization(note);

    return NoteRealmWithAllDescendants.fromNote(note, user);
  }

  @PatchMapping(path = "/{note}")
  @Transactional
  public NoteRealm updateNote(
      @PathVariable(name = "note") Note note,
      @Valid @ModelAttribute NoteAccessories noteAccessories)
      throws NoAccessRightException, IOException {
    final UserModel user = currentUserFetcher.getUser();
    user.getAuthorization().assertAuthorization(note);

    noteAccessories.setUpdatedAt(testabilitySettings.getCurrentUTCTimestamp());

    note.updateNoteContent(noteAccessories, user.getEntity());
    modelFactoryService.noteRepository.save(note);
    return new NoteViewer(user.getEntity(), note).toJsonObject();
  }

  @GetMapping("/{note}/statistics")
  public NoteStatistics statistics(@PathVariable("note") Note note) throws NoAccessRightException {
    final UserModel user = currentUserFetcher.getUser();
    user.getAuthorization().assertReadAuthorization(note);
    NoteStatistics statistics = new NoteStatistics();
    statistics.setReviewPoint(user.getReviewPointFor(note));
    statistics.note = new NoteViewer(user.getEntity(), note).toJsonObject();
    statistics.createdAt = note.getThing().getCreatedAt();
    return statistics;
  }

  @PostMapping("/search")
  @Transactional
  public List<Note> searchForLinkTarget(@Valid @RequestBody SearchTerm searchTerm) {
    SearchTermModel searchTermModel =
        modelFactoryService.toSearchTermModel(currentUserFetcher.getUser().getEntity(), searchTerm);
    return searchTermModel.searchForNotes();
  }

  @PostMapping(value = "/{note}/delete")
  @Transactional
  public Integer deleteNote(@PathVariable("note") Note note) throws NoAccessRightException {
    currentUserFetcher.getUser().getAuthorization().assertAuthorization(note);
    modelFactoryService.toNoteModel(note).destroy(testabilitySettings.getCurrentUTCTimestamp());
    modelFactoryService.entityManager.flush();
    return note.getId();
  }

  @PatchMapping(value = "/{note}/undo-delete")
  @Transactional
  public NoteRealm undoDeleteNote(@PathVariable("note") Note note) throws NoAccessRightException {
    UserModel user = currentUserFetcher.getUser();
    user.getAuthorization().assertAuthorization(note);
    modelFactoryService.toNoteModel(note).restore();
    modelFactoryService.entityManager.flush();

    return new NoteViewer(user.getEntity(), note).toJsonObject();
  }

  @GetMapping("/{note}/review-setting")
  public ReviewSetting getReviewSetting(Note note) {
    ReviewSetting reviewSetting = note.getMasterReviewSetting();
    if (reviewSetting == null) {
      reviewSetting = new ReviewSetting();
    }
    return reviewSetting;
  }

  @GetMapping("/{note}/position")
  public NotePositionViewedByUser getPosition(Note note) throws NoAccessRightException {
    UserModel user = currentUserFetcher.getUser();
    user.getAuthorization().assertAuthorization(note);
    return new NoteViewer(user.getEntity(), note).jsonNotePosition(note);
  }

  @PostMapping(value = "/{note}/review-setting")
  @Transactional
  public RedirectToNoteResponse updateReviewSetting(
      @PathVariable("note") Note note, @Valid @RequestBody ReviewSetting reviewSetting)
      throws NoAccessRightException {
    currentUserFetcher.getUser().getAuthorization().assertAuthorization(note);
    note.mergeMasterReviewSetting(reviewSetting);
    modelFactoryService.noteRepository.save(note);
    return new RedirectToNoteResponse(note.getId());
  }
}

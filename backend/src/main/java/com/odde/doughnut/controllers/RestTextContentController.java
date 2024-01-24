package com.odde.doughnut.controllers;

import com.odde.doughnut.controllers.json.NoteRealm;
import com.odde.doughnut.controllers.json.NoteUpdateDetailsDTO;
import com.odde.doughnut.controllers.json.NoteUpdateTopicDTO;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.exceptions.UnexpectedNoAccessRightException;
import com.odde.doughnut.factoryServices.ModelFactoryService;
import com.odde.doughnut.models.NoteViewer;
import com.odde.doughnut.models.UserModel;
import com.odde.doughnut.testability.TestabilitySettings;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import java.sql.Timestamp;
import java.util.function.Consumer;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/text_content")
class RestTextContentController {
  private final ModelFactoryService modelFactoryService;

  private UserModel currentUser;

  @Resource(name = "testabilitySettings")
  private final TestabilitySettings testabilitySettings;

  public RestTextContentController(
      ModelFactoryService modelFactoryService,
      UserModel currentUser,
      TestabilitySettings testabilitySettings) {
    this.modelFactoryService = modelFactoryService;
    this.currentUser = currentUser;
    this.testabilitySettings = testabilitySettings;
  }

  @PatchMapping(path = "/{note}/topic-constructor")
  @Transactional
  public NoteRealm updateNoteTopicConstructor(
      @PathVariable(name = "note") Note note, @Valid @ModelAttribute NoteUpdateTopicDTO topicDTO)
      throws UnexpectedNoAccessRightException {
    return updateNote(note, n -> n.setTopicConstructor(topicDTO.getTopicConstructor()));
  }

  @PatchMapping(path = "/{note}/details")
  @Transactional
  public NoteRealm updateNoteDetails(
      @PathVariable(name = "note") Note note,
      @Valid @ModelAttribute NoteUpdateDetailsDTO detailsDTO)
      throws UnexpectedNoAccessRightException {
    return updateNote(note, n -> n.setDetails(detailsDTO.getDetails()));
  }

  private NoteRealm updateNote(Note note, Consumer<Note> updateFunction)
      throws UnexpectedNoAccessRightException {
    currentUser.assertAuthorization(note);
    Timestamp currentUTCTimestamp = testabilitySettings.getCurrentUTCTimestamp();
    note.setUpdatedAt(currentUTCTimestamp);
    updateFunction.accept(note);
    modelFactoryService.save(note);
    return new NoteViewer(currentUser.getEntity(), note).toJsonObject();
  }
}

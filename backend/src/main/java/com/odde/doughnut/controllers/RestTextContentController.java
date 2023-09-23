package com.odde.doughnut.controllers;

import com.odde.doughnut.controllers.json.NoteRealm;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.TextContent;
import com.odde.doughnut.exceptions.UnexpectedNoAccessRightException;
import com.odde.doughnut.factoryServices.ModelFactoryService;
import com.odde.doughnut.models.NoteViewer;
import com.odde.doughnut.models.UserModel;
import com.odde.doughnut.testability.TestabilitySettings;
import java.sql.Timestamp;
import javax.annotation.Resource;
import javax.validation.Valid;
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

  @PatchMapping(path = "/{note}")
  @Transactional
  public NoteRealm updateNote(
      @PathVariable(name = "note") Note note, @Valid @ModelAttribute TextContent textContent)
      throws UnexpectedNoAccessRightException {
    currentUser.assertAuthorization(note);

    Timestamp currentUTCTimestamp = testabilitySettings.getCurrentUTCTimestamp();
    note.updateTextContent(currentUTCTimestamp, textContent);

    modelFactoryService.noteRepository.save(note);
    return new NoteViewer(currentUser.getEntity(), note).toJsonObject();
  }
}

package com.odde.doughnut.controllers;

import com.odde.doughnut.controllers.dto.NoteRealm;
import com.odde.doughnut.controllers.dto.NoteUpdateDetailsDTO;
import com.odde.doughnut.controllers.dto.NoteUpdateTitleDTO;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.exceptions.UnexpectedNoAccessRightException;
import com.odde.doughnut.factoryServices.ModelFactoryService;
import com.odde.doughnut.models.NoteViewer;
import com.odde.doughnut.models.UserModel;
import com.odde.doughnut.testability.TestabilitySettings;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.sql.Timestamp;
import java.util.function.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/text_content")
class RestTextContentController {
  private static final Logger logger = LoggerFactory.getLogger(RestTextContentController.class);

  private final ModelFactoryService modelFactoryService;

  private UserModel currentUser;

  private final HttpServletRequest request;

  @Resource(name = "testabilitySettings")
  private final TestabilitySettings testabilitySettings;

  public RestTextContentController(
      ModelFactoryService modelFactoryService,
      UserModel currentUser,
      TestabilitySettings testabilitySettings,
      HttpServletRequest request) {
    this.modelFactoryService = modelFactoryService;
    this.currentUser = currentUser;
    this.testabilitySettings = testabilitySettings;
    this.request = request;
  }

  @PatchMapping(path = "/{note}/title")
  @Transactional
  public NoteRealm updateNoteTitle(
      @PathVariable(name = "note") @Schema(type = "integer") Note note,
      @Valid @RequestBody NoteUpdateTitleDTO topicDTO)
      throws UnexpectedNoAccessRightException {
    return updateNote(note, n -> n.setTopicConstructor(topicDTO.getNewTitle()));
  }

  @PatchMapping(path = "/{note}/details")
  @Transactional
  public NoteRealm updateNoteDetails(
      @PathVariable(name = "note") @Schema(type = "integer") Note note,
      @Valid @RequestBody NoteUpdateDetailsDTO detailsDTO)
      throws UnexpectedNoAccessRightException {
    return updateNote(note, n -> n.setDetails(detailsDTO.getDetails()));
  }

  private NoteRealm updateNote(Note note, Consumer<Note> updateFunction)
      throws UnexpectedNoAccessRightException {
    String authorizationHeader = request.getHeader("Authorization");
    logger.debug("Authorization header: {}", authorizationHeader);
    UserModel userModel;
    if (currentUser == null) {
      String token = null;
      if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
        token = authorizationHeader.substring(7);
      } else if (authorizationHeader != null) {
        token = authorizationHeader;
      }
      logger.debug("Extracted token: {}", token);
      if (token == null) {
        throw new UnexpectedNoAccessRightException();
      }
      var userOpt = modelFactoryService.findUserByToken(token);
      if (userOpt.isEmpty()) {
        throw new UnexpectedNoAccessRightException();
      }
      userModel = new UserModel(userOpt.get(), modelFactoryService);
      userModel.assertAuthorization(note);
    } else {
      currentUser.assertAuthorization(note);
      userModel = currentUser;
    }
    Timestamp currentUTCTimestamp = testabilitySettings.getCurrentUTCTimestamp();
    note.setUpdatedAt(currentUTCTimestamp);
    updateFunction.accept(note);
    modelFactoryService.save(note);
    return new NoteViewer(userModel.getEntity(), note).toJsonObject();
  }
}

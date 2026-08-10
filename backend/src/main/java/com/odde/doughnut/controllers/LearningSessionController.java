package com.odde.doughnut.controllers;

import com.odde.doughnut.controllers.dto.CommissionLearningSessionRequest;
import com.odde.doughnut.controllers.dto.LearningSessionCommissionResponse;
import com.odde.doughnut.controllers.dto.LearningSessionRequestResponse;
import com.odde.doughnut.controllers.dto.RecordLearningSessionRequest;
import com.odde.doughnut.controllers.dto.RecordLearningSessionResponse;
import com.odde.doughnut.entities.Notebook;
import com.odde.doughnut.entities.repositories.NotebookRepository;
import com.odde.doughnut.exceptions.UnexpectedNoAccessRightException;
import com.odde.doughnut.services.AuthorizationService;
import com.odde.doughnut.services.LearningSessionService;
import com.odde.doughnut.testability.TestabilitySettings;
import com.odde.doughnut.utils.TimezoneUtils;
import java.sql.Timestamp;
import java.time.ZoneId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.annotation.SessionScope;
import org.springframework.web.server.ResponseStatusException;

@RestController
@SessionScope
@RequestMapping("/api/learning-sessions")
class LearningSessionController {

  private final LearningSessionService learningSessionService;
  private final NotebookRepository notebookRepository;
  private final AuthorizationService authorizationService;
  private final TestabilitySettings testabilitySettings;

  @Autowired
  public LearningSessionController(
      LearningSessionService learningSessionService,
      NotebookRepository notebookRepository,
      AuthorizationService authorizationService,
      TestabilitySettings testabilitySettings) {
    this.learningSessionService = learningSessionService;
    this.notebookRepository = notebookRepository;
    this.authorizationService = authorizationService;
    this.testabilitySettings = testabilitySettings;
  }

  @GetMapping("/request")
  @Transactional(readOnly = true)
  public LearningSessionRequestResponse request(
      @RequestParam(value = "notebookId") Integer notebookId,
      @RequestParam(value = "timezone") String timezone)
      throws UnexpectedNoAccessRightException {
    Notebook notebook = authorizedNotebook(notebookId);
    ZoneId zoneId = TimezoneUtils.parseTimezone(timezone);
    Timestamp now = testabilitySettings.getCurrentUTCTimestamp();
    return learningSessionService.request(
        authorizationService.getCurrentUser(), notebook, now, zoneId);
  }

  @PostMapping("/commission")
  @Transactional
  public LearningSessionCommissionResponse commission(
      @RequestBody CommissionLearningSessionRequest body,
      @RequestParam(value = "timezone") String timezone)
      throws UnexpectedNoAccessRightException {
    Notebook notebook = authorizedNotebook(body.notebookId);
    ZoneId zoneId = TimezoneUtils.parseTimezone(timezone);
    Timestamp now = testabilitySettings.getCurrentUTCTimestamp();
    return learningSessionService.commission(
        authorizationService.getCurrentUser(), notebook, now, zoneId);
  }

  @PostMapping("/record")
  @Transactional
  public RecordLearningSessionResponse record(
      @RequestBody RecordLearningSessionRequest body,
      @RequestParam(value = "timezone") String timezone)
      throws UnexpectedNoAccessRightException {
    Notebook notebook = authorizedNotebook(body.notebookId);
    Timestamp now = testabilitySettings.getCurrentUTCTimestamp();
    return learningSessionService.record(
        authorizationService.getCurrentUser(), notebook, body.reportMarkdown, now);
  }

  private Notebook authorizedNotebook(Integer notebookId) throws UnexpectedNoAccessRightException {
    authorizationService.assertLoggedIn();
    Notebook notebook =
        notebookRepository
            .findById(notebookId)
            .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Notebook not found."));
    authorizationService.assertAuthorization(notebook);
    return notebook;
  }
}

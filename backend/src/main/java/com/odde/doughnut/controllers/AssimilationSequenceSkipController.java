package com.odde.doughnut.controllers;

import com.odde.doughnut.controllers.dto.AssimilationSequenceSkipRequestDTO;
import com.odde.doughnut.entities.AssimilationSequenceSkip;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.User;
import com.odde.doughnut.entities.repositories.AssimilationSequenceSkipRepository;
import com.odde.doughnut.exceptions.UnexpectedNoAccessRightException;
import com.odde.doughnut.factoryServices.EntityPersister;
import com.odde.doughnut.services.AuthorizationService;
import com.odde.doughnut.testability.TestabilitySettings;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/assimilation-sequence-skips")
class AssimilationSequenceSkipController {
  private final EntityPersister entityPersister;
  private final AssimilationSequenceSkipRepository skipRepository;
  private final TestabilitySettings testabilitySettings;
  private final AuthorizationService authorizationService;

  public AssimilationSequenceSkipController(
      EntityPersister entityPersister,
      AssimilationSequenceSkipRepository skipRepository,
      TestabilitySettings testabilitySettings,
      AuthorizationService authorizationService) {
    this.entityPersister = entityPersister;
    this.skipRepository = skipRepository;
    this.testabilitySettings = testabilitySettings;
    this.authorizationService = authorizationService;
  }

  @PostMapping(path = "")
  @Transactional
  public AssimilationSequenceSkip create(@RequestBody AssimilationSequenceSkipRequestDTO request)
      throws UnexpectedNoAccessRightException {
    authorizationService.assertLoggedIn();
    Note note = entityPersister.find(Note.class, request.noteId);
    authorizationService.assertReadAuthorization(note);
    User user = authorizationService.getCurrentUser();
    String propertyKey = request.propertyKey == null ? "" : request.propertyKey;
    return skipRepository
        .findByUserAndNoteAndPropertyKey(user, note, propertyKey)
        .orElseGet(() -> persistSkip(user, note, propertyKey));
  }

  private AssimilationSequenceSkip persistSkip(User user, Note note, String propertyKey) {
    AssimilationSequenceSkip skip = new AssimilationSequenceSkip();
    skip.setUser(user);
    skip.setNote(note);
    skip.setPropertyKey(propertyKey);
    skip.setSkippedAt(testabilitySettings.getCurrentUTCTimestamp());
    return entityPersister.save(skip);
  }
}

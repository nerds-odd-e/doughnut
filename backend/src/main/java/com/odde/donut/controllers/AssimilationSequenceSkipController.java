package com.odde.donut.controllers;

import com.odde.donut.controllers.dto.AssimilationSequenceSkipRequestDTO;
import com.odde.donut.entities.AssimilationSequenceSkip;
import com.odde.donut.entities.Note;
import com.odde.donut.entities.User;
import com.odde.donut.entities.repositories.AssimilationSequenceSkipRepository;
import com.odde.donut.exceptions.UnexpectedNoAccessRightException;
import com.odde.donut.factoryServices.EntityPersister;
import com.odde.donut.services.AuthorizationService;
import com.odde.donut.testability.TestabilitySettings;
import java.util.Optional;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
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
    SkipGrain grain = authorizedSkipGrain(request);
    return findSkip(grain).orElseGet(() -> persistSkip(grain));
  }

  @DeleteMapping(path = "")
  @Transactional
  public void deleteAssimilationSequenceSkip(
      @RequestBody AssimilationSequenceSkipRequestDTO request)
      throws UnexpectedNoAccessRightException {
    findSkip(authorizedSkipGrain(request)).ifPresent(entityPersister::remove);
  }

  private record SkipGrain(User user, Note note, String propertyKey) {}

  private SkipGrain authorizedSkipGrain(AssimilationSequenceSkipRequestDTO request)
      throws UnexpectedNoAccessRightException {
    authorizationService.assertLoggedIn();
    Note note = entityPersister.find(Note.class, request.noteId);
    authorizationService.assertReadAuthorization(note);
    return new SkipGrain(
        authorizationService.getCurrentUser(),
        note,
        request.propertyKey == null ? "" : request.propertyKey);
  }

  private Optional<AssimilationSequenceSkip> findSkip(SkipGrain grain) {
    return skipRepository.findByUserAndNoteAndPropertyKey(
        grain.user(), grain.note(), grain.propertyKey());
  }

  private AssimilationSequenceSkip persistSkip(SkipGrain grain) {
    AssimilationSequenceSkip skip = new AssimilationSequenceSkip();
    skip.setUser(grain.user());
    skip.setNote(grain.note());
    skip.setPropertyKey(grain.propertyKey());
    skip.setSkippedAt(testabilitySettings.getCurrentUTCTimestamp());
    return entityPersister.save(skip);
  }
}

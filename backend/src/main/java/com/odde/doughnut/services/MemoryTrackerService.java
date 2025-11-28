package com.odde.doughnut.services;

import com.odde.doughnut.controllers.dto.AnswerSpellingDTO;
import com.odde.doughnut.controllers.dto.InitialInfo;
import com.odde.doughnut.controllers.dto.SpellingResultDTO;
import com.odde.doughnut.entities.MemoryTracker;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.PredefinedQuestion;
import com.odde.doughnut.entities.User;
import com.odde.doughnut.entities.repositories.MemoryTrackerRepository;
import com.odde.doughnut.factoryServices.EntityPersister;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class MemoryTrackerService {
  private final EntityPersister entityPersister;
  private final UserService userService;
  private final MemoryTrackerRepository memoryTrackerRepository;

  public MemoryTrackerService(
      EntityPersister entityPersister,
      UserService userService,
      MemoryTrackerRepository memoryTrackerRepository) {
    this.entityPersister = entityPersister;
    this.userService = userService;
    this.memoryTrackerRepository = memoryTrackerRepository;
  }

  public List<MemoryTracker> findLast100ByUser(Integer userId) {
    return memoryTrackerRepository.findLast100ByUser(userId);
  }

  public List<MemoryTracker> findLast100ReviewedByUser(Integer userId) {
    return memoryTrackerRepository.findLast100ReviewedByUser(userId);
  }

  public List<MemoryTracker> assimilate(
      InitialInfo initialInfo, User currentUser, Timestamp currentTime) {
    Note note = entityPersister.find(Note.class, initialInfo.noteId);
    MemoryTracker memoryTracker =
        createMemoryTracker(
            note,
            currentUser,
            currentTime,
            initialInfo.skipMemoryTracking != null ? initialInfo.skipMemoryTracking : false,
            false);

    List<MemoryTracker> trackers = new ArrayList<>();
    trackers.add(memoryTracker);

    if (note.getRecallSetting().getRememberSpelling()) {
      MemoryTracker spellingTracker =
          createMemoryTracker(
              note,
              currentUser,
              currentTime,
              initialInfo.skipMemoryTracking != null ? initialInfo.skipMemoryTracking : false,
              true);
      trackers.add(spellingTracker);
    }

    return trackers;
  }

  private MemoryTracker createMemoryTracker(
      Note note,
      User currentUser,
      Timestamp currentTime,
      boolean skipMemoryTracking,
      boolean isSpelling) {
    MemoryTracker memoryTracker = MemoryTracker.buildMemoryTrackerForNote(note);
    memoryTracker.setRemovedFromTracking(skipMemoryTracking);
    memoryTracker.setSpelling(isSpelling);
    memoryTracker.setUser(currentUser);
    memoryTracker.setAssimilatedAt(currentTime);
    memoryTracker.setLastRecalledAt(currentTime);
    updateForgettingCurve(memoryTracker, 0);
    return memoryTracker;
  }

  public void updateForgettingCurve(MemoryTracker memoryTracker, int adjustment) {
    memoryTracker.setForgettingCurveIndex(memoryTracker.getForgettingCurveIndex() + adjustment);
    memoryTracker.setNextRecallAt(memoryTracker.calculateNextRecallAt());
    entityPersister.save(memoryTracker);
  }

  public void updateMemoryTrackerAfterAnsweringQuestion(
      User user,
      Timestamp currentUTCTimestamp,
      Boolean correct,
      PredefinedQuestion predefinedQuestion) {
    List<MemoryTracker> memoryTrackers =
        userService.getMemoryTrackersFor(user, predefinedQuestion.getNote());
    memoryTrackers.stream()
        .filter(
            tracker -> {
              Boolean trackerSpelling = tracker.getSpelling();
              return !Boolean.TRUE.equals(trackerSpelling);
            })
        .findFirst()
        .ifPresent(memoryTracker -> markAsRepeated(currentUTCTimestamp, correct, memoryTracker));
  }

  public void markAsRepeated(
      Timestamp currentUTCTimestamp, Boolean correct, MemoryTracker memoryTracker) {
    memoryTracker.markAsRepeated(currentUTCTimestamp, correct);
    entityPersister.save(memoryTracker);
  }

  public SpellingResultDTO answerSpelling(
      MemoryTracker memoryTracker,
      AnswerSpellingDTO answerSpellingDTO,
      User user,
      Timestamp currentUTCTimestamp) {
    String spellingAnswer = answerSpellingDTO.getSpellingAnswer();
    Note note = memoryTracker.getNote();
    Boolean correct = note.matchAnswer(spellingAnswer);
    markAsRepeated(currentUTCTimestamp, correct, memoryTracker);
    return new SpellingResultDTO(note, spellingAnswer, correct);
  }
}

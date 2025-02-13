package com.odde.doughnut.services;

import com.odde.doughnut.controllers.dto.AnswerSpellingDTO;
import com.odde.doughnut.controllers.dto.InitialInfo;
import com.odde.doughnut.controllers.dto.SpellingResultDTO;
import com.odde.doughnut.entities.MemoryTracker;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.RecallPrompt;
import com.odde.doughnut.entities.User;
import com.odde.doughnut.factoryServices.ModelFactoryService;
import com.odde.doughnut.models.UserModel;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

public class MemoryTrackerService {
  private final ModelFactoryService modelFactoryService;

  public MemoryTrackerService(ModelFactoryService modelFactoryService) {
    this.modelFactoryService = modelFactoryService;
  }

  public List<MemoryTracker> assimilate(
      InitialInfo initialInfo, User currentUser, Timestamp currentTime) {
    Note note = modelFactoryService.entityManager.find(Note.class, initialInfo.noteId);
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
    modelFactoryService.save(memoryTracker);
  }

  public void updateMemoryTrackerAfterAnsweringQuestion(
      User user, Timestamp currentUTCTimestamp, Boolean correct, RecallPrompt recallPrompt) {
    UserModel userModel = new UserModel(user, modelFactoryService);
    Boolean checkSpell = recallPrompt.getPredefinedQuestion().getBareQuestion().getCheckSpell();
    List<MemoryTracker> memoryTrackers =
        userModel.getMemoryTrackersFor(recallPrompt.getPredefinedQuestion().getNote());
    memoryTrackers.stream()
        .filter(
            tracker -> {
              Boolean trackerSpelling = tracker.getSpelling();
              return Boolean.TRUE.equals(checkSpell) == Boolean.TRUE.equals(trackerSpelling);
            })
        .findFirst()
        .ifPresent(memoryTracker -> markAsRepeated(currentUTCTimestamp, correct, memoryTracker));
  }

  public void markAsRepeated(
      Timestamp currentUTCTimestamp, Boolean correct, MemoryTracker memoryTracker) {
    memoryTracker.markAsRepeated(currentUTCTimestamp, correct);
    modelFactoryService.save(memoryTracker);
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

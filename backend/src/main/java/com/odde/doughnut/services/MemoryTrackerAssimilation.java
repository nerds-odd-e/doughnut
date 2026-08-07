package com.odde.doughnut.services;

import com.odde.doughnut.controllers.dto.AssimilationRequestDTO;
import com.odde.doughnut.entities.MemoryTracker;
import com.odde.doughnut.entities.MemoryTrackerType;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.User;
import com.odde.doughnut.factoryServices.EntityPersister;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

/** Creates memory trackers for assimilate requests. */
final class MemoryTrackerAssimilation {
  private final EntityPersister entityPersister;
  private final UserService userService;
  private final MemoryTrackerService memoryTrackerService;

  MemoryTrackerAssimilation(
      EntityPersister entityPersister,
      UserService userService,
      MemoryTrackerService memoryTrackerService) {
    this.entityPersister = entityPersister;
    this.userService = userService;
    this.memoryTrackerService = memoryTrackerService;
  }

  List<MemoryTracker> assimilate(
      AssimilationRequestDTO request, User currentUser, Timestamp currentTime) {
    Note note = entityPersister.find(Note.class, request.noteId);
    List<MemoryTracker> existingTrackers = userService.getMemoryTrackersFor(currentUser, note);
    boolean skipMemoryTracking =
        request.skipMemoryTracking != null ? request.skipMemoryTracking : false;

    if (Boolean.TRUE.equals(request.assimilateAsCommissioned)) {
      if (request.propertyKey != null && !request.propertyKey.isEmpty()) {
        return List.of();
      }
      boolean commissionedExists =
          existingTrackers.stream()
              .filter(MemoryTracker::isNoteLevelTracker)
              .anyMatch(mt -> mt.getType() == MemoryTrackerType.COMMISSIONED);
      if (commissionedExists) {
        return List.of();
      }
      return List.of(
          createNoteLevelTracker(
              note, currentUser, currentTime, skipMemoryTracking, MemoryTrackerType.COMMISSIONED));
    }

    if (request.propertyKey != null && !request.propertyKey.isEmpty()) {
      boolean propertyTrackerExists =
          existingTrackers.stream().anyMatch(mt -> request.propertyKey.equals(mt.getPropertyKey()));
      if (propertyTrackerExists) {
        return List.of();
      }
      return List.of(
          initializeNewTracker(
              MemoryTracker.buildMemoryTrackerForProperty(note, request.propertyKey),
              currentUser,
              currentTime,
              skipMemoryTracking,
              MemoryTrackerType.UNDERSTANDING));
    }

    List<MemoryTracker> existingNoteLevelTrackers =
        existingTrackers.stream()
            .filter(MemoryTracker::isNoteLevelTracker)
            .filter(mt -> mt.getType() != MemoryTrackerType.COMMISSIONED)
            .toList();

    boolean addSpellingOnly =
        !existingNoteLevelTrackers.isEmpty()
            && Boolean.TRUE.equals(note.getRecallSetting().getRememberSpelling())
            && existingNoteLevelTrackers.stream().noneMatch(MemoryTracker::isSpelling);

    if (addSpellingOnly) {
      return List.of(
          createNoteLevelTracker(
              note, currentUser, currentTime, skipMemoryTracking, MemoryTrackerType.SPELLING));
    }

    if (!existingNoteLevelTrackers.isEmpty()) {
      return List.of();
    }

    List<MemoryTracker> trackers = new ArrayList<>();
    trackers.add(
        createNoteLevelTracker(
            note, currentUser, currentTime, skipMemoryTracking, MemoryTrackerType.UNDERSTANDING));
    if (Boolean.TRUE.equals(note.getRecallSetting().getRememberSpelling())) {
      trackers.add(
          createNoteLevelTracker(
              note, currentUser, currentTime, skipMemoryTracking, MemoryTrackerType.SPELLING));
    }
    return trackers;
  }

  private MemoryTracker createNoteLevelTracker(
      Note note,
      User currentUser,
      Timestamp currentTime,
      boolean skipMemoryTracking,
      MemoryTrackerType type) {
    return initializeNewTracker(
        MemoryTracker.buildMemoryTrackerForNote(note),
        currentUser,
        currentTime,
        skipMemoryTracking,
        type);
  }

  private MemoryTracker initializeNewTracker(
      MemoryTracker memoryTracker,
      User currentUser,
      Timestamp currentTime,
      boolean skipMemoryTracking,
      MemoryTrackerType type) {
    memoryTracker.setRemovedFromTracking(skipMemoryTracking);
    memoryTracker.setType(type);
    memoryTracker.setUser(currentUser);
    memoryTracker.setAssimilatedAt(currentTime);
    memoryTracker.setLastRecalledAt(currentTime);
    memoryTrackerService.updateForgettingCurve(memoryTracker, 0.0f);
    return memoryTracker;
  }
}

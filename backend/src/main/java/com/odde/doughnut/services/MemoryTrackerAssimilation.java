package com.odde.doughnut.services;

import com.odde.doughnut.controllers.dto.AssimilationRequestDTO;
import com.odde.doughnut.entities.MemoryTracker;
import com.odde.doughnut.entities.MemoryTrackerType;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.User;
import com.odde.doughnut.entities.repositories.AssimilationSequenceSkipRepository;
import com.odde.doughnut.factoryServices.EntityPersister;
import java.sql.Timestamp;
import java.util.List;

/** Creates memory trackers for assimilate requests. */
final class MemoryTrackerAssimilation {
  private final EntityPersister entityPersister;
  private final UserService userService;
  private final MemoryTrackerService memoryTrackerService;
  private final AssimilationSequenceSkipRepository skipRepository;

  MemoryTrackerAssimilation(
      EntityPersister entityPersister,
      UserService userService,
      MemoryTrackerService memoryTrackerService,
      AssimilationSequenceSkipRepository skipRepository) {
    this.entityPersister = entityPersister;
    this.userService = userService;
    this.memoryTrackerService = memoryTrackerService;
    this.skipRepository = skipRepository;
  }

  List<MemoryTracker> assimilate(
      AssimilationRequestDTO request, User currentUser, Timestamp currentTime) {
    Note note = entityPersister.find(Note.class, request.noteId);
    List<MemoryTracker> existingTrackers = userService.getMemoryTrackersFor(currentUser, note);
    boolean skipMemoryTracking =
        request.skipMemoryTracking != null ? request.skipMemoryTracking : false;

    if (Boolean.TRUE.equals(request.assimilateAsCommissioned)) {
      return assimilateAsNoteLevelType(
          request,
          existingTrackers,
          note,
          currentUser,
          currentTime,
          skipMemoryTracking,
          MemoryTrackerType.COMMISSIONED);
    }

    if (Boolean.TRUE.equals(request.assimilateAsSpelling)) {
      return assimilateAsNoteLevelType(
          request,
          existingTrackers,
          note,
          currentUser,
          currentTime,
          skipMemoryTracking,
          MemoryTrackerType.SPELLING);
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

    return assimilateAsNoteLevelType(
        request,
        existingTrackers,
        note,
        currentUser,
        currentTime,
        skipMemoryTracking,
        MemoryTrackerType.UNDERSTANDING);
  }

  private List<MemoryTracker> assimilateAsNoteLevelType(
      AssimilationRequestDTO request,
      List<MemoryTracker> existingTrackers,
      Note note,
      User currentUser,
      Timestamp currentTime,
      boolean skipMemoryTracking,
      MemoryTrackerType type) {
    if (request.propertyKey != null && !request.propertyKey.isEmpty()) {
      return List.of();
    }
    if (hasNoteLevelType(existingTrackers, type)) {
      return List.of();
    }
    return List.of(
        createNoteLevelTracker(note, currentUser, currentTime, skipMemoryTracking, type));
  }

  private static boolean hasNoteLevelType(List<MemoryTracker> trackers, MemoryTrackerType type) {
    return trackers.stream()
        .filter(MemoryTracker::isNoteLevelTracker)
        .anyMatch(mt -> mt.getType() == type);
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
    if (type == MemoryTrackerType.UNDERSTANDING) {
      deleteMatchingSequenceSkip(currentUser, memoryTracker);
    }
    return memoryTracker;
  }

  private void deleteMatchingSequenceSkip(User currentUser, MemoryTracker memoryTracker) {
    skipRepository
        .findByUserAndNoteAndPropertyKey(
            currentUser, memoryTracker.getNote(), memoryTracker.getPropertyKey())
        .ifPresent(entityPersister::remove);
  }
}

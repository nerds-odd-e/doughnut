package com.odde.donut.services;

import com.odde.donut.entities.MemoryTracker;
import com.odde.donut.entities.Note;
import com.odde.donut.entities.PropertyFocus;
import com.odde.donut.entities.repositories.MemoryTrackerRepository;
import com.odde.donut.factoryServices.EntityPersister;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

/** Keeps property memory trackers on their property when the note's frontmatter changes. */
@Service
public class PropertyMemoryTrackerService {
  private final EntityPersister entityPersister;
  private final UserService userService;
  private final MemoryTrackerRepository memoryTrackerRepository;

  public PropertyMemoryTrackerService(
      EntityPersister entityPersister,
      UserService userService,
      MemoryTrackerRepository memoryTrackerRepository) {
    this.entityPersister = entityPersister;
    this.userService = userService;
    this.memoryTrackerRepository = memoryTrackerRepository;
  }

  public void updatePropertyKey(MemoryTracker memoryTracker, String newPropertyKey) {
    if (!memoryTracker.getNote().isAvailable()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Memory tracker is deleted");
    }
    if (memoryTracker.isNoteLevelTracker()) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "Cannot rename note-level memory tracker");
    }
    if (newPropertyKey == null || newPropertyKey.isBlank()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Property key must not be blank");
    }
    if (newPropertyKey.equals(memoryTracker.getPropertyKey())) {
      return;
    }
    PropertyFocus renamed = new PropertyFocus(newPropertyKey, memoryTracker.getPropertyValue());
    boolean conflict =
        userService.getMemoryTrackersFor(memoryTracker.getUser(), memoryTracker.getNote()).stream()
            .filter(MemoryTracker::isActive)
            .filter(mt -> !mt.isSpelling())
            .filter(mt -> !mt.getId().equals(memoryTracker.getId()))
            .anyMatch(mt -> renamed.equals(mt.propertyFocus()));
    if (conflict) {
      throw new ResponseStatusException(
          HttpStatus.CONFLICT,
          "A property memory tracker for \"" + newPropertyKey + "\" already exists on this note.");
    }
    memoryTracker.setPropertyKey(newPropertyKey);
    entityPersister.save(memoryTracker);
  }

  /**
   * The single value of {@code note}'s {@code propertyKey} became {@code propertyValue}, one value
   * of a list: every learner's tracker of that single value now tracks that list value.
   */
  public void followPropertyValue(Note note, String propertyKey, String propertyValue) {
    PropertyFocus singleValue = new PropertyFocus(propertyKey, "");
    memoryTrackerRepository.findByNote_IdIn(List.of(note.getId())).stream()
        .filter(tracker -> singleValue.equals(tracker.propertyFocus()))
        .forEach(
            tracker -> {
              tracker.setPropertyValue(propertyValue);
              entityPersister.save(tracker);
            });
  }

  /**
   * Moves every learner's tracker of {@code source}'s single-valued {@code propertyKey} property
   * (matched case-insensitively) onto {@code relationshipNote} as a note-level tracker.
   */
  public void movePropertyTrackersOntoRelationshipNote(
      Note source, String propertyKey, Note relationshipNote) {
    memoryTrackerRepository.findByNote_IdIn(List.of(source.getId())).stream()
        .filter(tracker -> propertyKey.equalsIgnoreCase(tracker.getPropertyKey()))
        .filter(tracker -> tracker.getPropertyValue().isEmpty())
        .forEach(
            tracker -> {
              tracker.setNote(relationshipNote);
              tracker.setPropertyKey("");
              entityPersister.merge(tracker);
            });
  }

  /**
   * Moves every learner's note-level understanding tracker on {@code relationNote} onto {@code
   * focus} of {@code sourceNote}, unless that learner already tracks that focus there. Every other
   * tracker on {@code relationNote} (spelling, commissioned, or property-level) is left for the DB
   * {@code ON DELETE CASCADE} to remove with the relationship note; those are detached here so
   * Hibernate's persistence context does not keep a managed reference to a note about to be removed
   * (its own pre-flush transient-dependency check does not know about that DB-level cascade).
   */
  public void rehomeNoteLevelTrackersToProperty(
      Note relationNote, Note sourceNote, PropertyFocus focus) {
    List<MemoryTracker> sourceTrackers =
        memoryTrackerRepository.findByNote_IdIn(List.of(sourceNote.getId()));
    memoryTrackerRepository
        .findByNote_IdIn(List.of(relationNote.getId()))
        .forEach(
            tracker -> {
              if (tracker.isUnderstanding()
                  && tracker.isNoteLevelTracker()
                  && sourceTrackers.stream()
                      .noneMatch(
                          existing -> sameLearnersUnderstandingOf(existing, tracker, focus))) {
                tracker.setNote(sourceNote);
                tracker.setPropertyKey(focus.key());
                tracker.setPropertyValue(focus.value());
                entityPersister.merge(tracker);
              } else {
                entityPersister.detach(tracker);
              }
            });
  }

  private static boolean sameLearnersUnderstandingOf(
      MemoryTracker existing, MemoryTracker tracker, PropertyFocus focus) {
    return existing.isUnderstanding()
        && existing.getUser().getId().equals(tracker.getUser().getId())
        && focus.equals(existing.propertyFocus());
  }
}

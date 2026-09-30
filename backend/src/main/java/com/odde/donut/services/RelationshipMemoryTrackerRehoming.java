package com.odde.donut.services;

import com.odde.donut.entities.MemoryTracker;
import com.odde.donut.entities.Note;
import com.odde.donut.entities.repositories.MemoryTrackerRepository;
import com.odde.donut.factoryServices.EntityPersister;
import java.util.List;

/**
 * Moves learners' memory trackers between a source note's property and the relationship note that
 * stands for it, in both directions, keeping each tracker's learning state and recall history.
 */
final class RelationshipMemoryTrackerRehoming {
  private final MemoryTrackerRepository memoryTrackerRepository;
  private final EntityPersister entityPersister;

  RelationshipMemoryTrackerRehoming(
      MemoryTrackerRepository memoryTrackerRepository, EntityPersister entityPersister) {
    this.memoryTrackerRepository = memoryTrackerRepository;
    this.entityPersister = entityPersister;
  }

  /**
   * Moves every learner's tracker of {@code source}'s {@code propertyKey} property onto {@code
   * relationshipNote} as a note-level tracker.
   */
  void movePropertyTrackersOntoRelationshipNote(
      Note source, String propertyKey, Note relationshipNote) {
    trackersOf(source).stream()
        .filter(tracker -> propertyKey.equalsIgnoreCase(tracker.getPropertyKey()))
        .forEach(tracker -> rehome(tracker, relationshipNote, ""));
  }

  /**
   * Moves every learner's note-level understanding tracker onto the source property. Every other
   * tracker on {@code relationNote} (spelling, commissioned, or property-level) is left for the DB
   * {@code ON DELETE CASCADE} to remove with the relationship note; those are detached here so
   * Hibernate's persistence context does not keep a managed reference to a note about to be removed
   * (its own pre-flush transient-dependency check does not know about that DB-level cascade).
   */
  void moveNoteLevelTrackersOntoSourceProperty(
      Note relationNote, Note sourceNote, String propertyKey) {
    trackersOf(relationNote)
        .forEach(
            tracker -> {
              if (tracker.isUnderstanding() && tracker.isNoteLevelTracker()) {
                rehome(tracker, sourceNote, propertyKey);
              } else {
                entityPersister.detach(tracker);
              }
            });
  }

  private List<MemoryTracker> trackersOf(Note note) {
    return memoryTrackerRepository.findByNote_IdIn(List.of(note.getId()));
  }

  private void rehome(MemoryTracker tracker, Note note, String propertyKey) {
    tracker.setNote(note);
    tracker.setPropertyKey(propertyKey);
    entityPersister.merge(tracker);
  }
}

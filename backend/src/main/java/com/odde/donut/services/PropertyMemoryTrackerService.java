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
}

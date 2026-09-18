package com.odde.donut.services;

import com.odde.donut.algorithms.NoteReferenceResolution;
import com.odde.donut.entities.Note;
import com.odde.donut.entities.NotePropertyIndex;
import com.odde.donut.entities.Subscription;
import com.odde.donut.entities.User;
import com.odde.donut.entities.repositories.MemoryTrackerRepository;
import com.odde.donut.entities.repositories.NotePropertyIndexRepository;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Stream;
import org.springframework.stereotype.Service;

@Service
public class UnassimilatedPropertyService {

  private final NotePropertyIndexRepository notePropertyIndexRepository;
  private final MemoryTrackerRepository memoryTrackerRepository;
  private final WikiLinkResolver wikiLinkResolver;
  private final NotePropertyIndexService notePropertyIndexService;

  public UnassimilatedPropertyService(
      NotePropertyIndexRepository notePropertyIndexRepository,
      MemoryTrackerRepository memoryTrackerRepository,
      WikiLinkResolver wikiLinkResolver,
      NotePropertyIndexService notePropertyIndexService) {
    this.notePropertyIndexRepository = notePropertyIndexRepository;
    this.memoryTrackerRepository = memoryTrackerRepository;
    this.wikiLinkResolver = wikiLinkResolver;
    this.notePropertyIndexService = notePropertyIndexService;
  }

  public int countUnassimilatedPropertiesForUser(User user) {
    return notePropertyIndexRepository.countUnassimilatedPropertiesForOwnership(
        user.getId(), user.getOwnership().getId());
  }

  public int countUnassimilatedPropertiesForSubscription(Subscription subscription) {
    return notePropertyIndexRepository.countUnassimilatedPropertiesForNotebook(
        subscription.getUser().getId(), subscription.getNotebook().getId());
  }

  public Stream<AssimilationUnit> streamUnassimilatedPropertiesForUser(
      User user, Predicate<AssimilationUnit> canStillBeNext) {
    return assimilable(
        notePropertyIndexRepository.streamUnassimilatedPropertiesForOwnership(
            user.getId(), user.getOwnership().getId()),
        canStillBeNext,
        user);
  }

  public Stream<AssimilationUnit> streamUnassimilatedPropertiesForSubscription(
      Subscription subscription, Predicate<AssimilationUnit> canStillBeNext) {
    User viewer = subscription.getUser();
    return assimilable(
        notePropertyIndexRepository.streamUnassimilatedPropertiesForNotebook(
            viewer.getId(), subscription.getNotebook().getId()),
        canStillBeNext,
        viewer);
  }

  /** The gate is evaluated only for ordered candidates that can still be the next unit. */
  private Stream<AssimilationUnit> assimilable(
      Stream<AssimilationUnit> candidates,
      Predicate<AssimilationUnit> canStillBeNext,
      User viewer) {
    return candidates.takeWhile(canStillBeNext).filter(unit -> !isGated(unit, viewer));
  }

  /**
   * A property unit is gated while any sibling row in its (note, propertyKey) list-property family
   * currently resolves, for {@code viewer}, to a note that isn't yet handled (no completed
   * note-level UNDERSTANDING tracker). A reference that doesn't currently resolve (deleted target,
   * ambiguous, missing) never gates. Note-level units (propertyKey == null) are never gated.
   */
  private boolean isGated(AssimilationUnit unit, User viewer) {
    if (!unit.isPropertyLevel()) {
      return false;
    }
    List<NotePropertyIndex> siblings =
        notePropertyIndexRepository.findByNote_IdAndPropertyKey(
            unit.note().getId(), unit.propertyKey());
    for (var reference :
        notePropertyIndexService.authoredReferencesForProperty(
            unit.note(), unit.propertyKey(), siblings)) {
      NoteReferenceResolution resolution =
          wikiLinkResolver.resolveReference(reference, unit.note(), viewer);
      if (resolution instanceof NoteReferenceResolution.Resolved resolved
          && !isHandled(resolved.destinationNote(), viewer)) {
        return true;
      }
    }
    return false;
  }

  private boolean isHandled(Note destinationNote, User viewer) {
    return destinationNote.isAvailable()
        && memoryTrackerRepository.existsCompletedNoteLevelUnderstandingTracker(
            destinationNote.getId(), viewer.getId());
  }
}

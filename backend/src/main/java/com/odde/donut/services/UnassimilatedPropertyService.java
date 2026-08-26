package com.odde.donut.services;

import com.odde.donut.algorithms.PropertyKeyNaming;
import com.odde.donut.entities.NotePropertyIndex;
import com.odde.donut.entities.Subscription;
import com.odde.donut.entities.User;
import com.odde.donut.entities.repositories.NotePropertyIndexRepository;
import java.util.stream.Stream;
import org.springframework.stereotype.Service;

@Service
public class UnassimilatedPropertyService {

  private final NotePropertyIndexRepository notePropertyIndexRepository;

  public UnassimilatedPropertyService(NotePropertyIndexRepository notePropertyIndexRepository) {
    this.notePropertyIndexRepository = notePropertyIndexRepository;
  }

  public int countUnassimilatedPropertiesForUser(User user) {
    return countAssimilable(
        notePropertyIndexRepository.streamUnassimilatedPropertiesForOwnership(
            user.getId(), user.getOwnership().getId()));
  }

  public int countUnassimilatedPropertiesForSubscription(Subscription subscription) {
    return countAssimilable(
        notePropertyIndexRepository.streamUnassimilatedPropertiesForNotebook(
            subscription.getUser().getId(), subscription.getNotebook().getId()));
  }

  public Stream<AssimilationUnit> streamUnassimilatedPropertiesForUser(User user) {
    return streamAssimilable(
            notePropertyIndexRepository.streamUnassimilatedPropertiesForOwnership(
                user.getId(), user.getOwnership().getId()))
        .map(UnassimilatedPropertyService::toPropertyUnit);
  }

  public Stream<AssimilationUnit> streamUnassimilatedPropertiesForSubscription(
      Subscription subscription) {
    return streamAssimilable(
            notePropertyIndexRepository.streamUnassimilatedPropertiesForNotebook(
                subscription.getUser().getId(), subscription.getNotebook().getId()))
        .map(UnassimilatedPropertyService::toPropertyUnit);
  }

  private static int countAssimilable(Stream<NotePropertyIndex> unfiltered) {
    try (Stream<NotePropertyIndex> stream = streamAssimilable(unfiltered)) {
      return (int) stream.count();
    }
  }

  private static Stream<NotePropertyIndex> streamAssimilable(Stream<NotePropertyIndex> unfiltered) {
    return unfiltered.filter(
        index -> !PropertyKeyNaming.isReservedStructuralKey(index.getPropertyKey()));
  }

  private static AssimilationUnit toPropertyUnit(NotePropertyIndex index) {
    return AssimilationUnit.forProperty(index.getNote(), index.getPropertyKey());
  }
}

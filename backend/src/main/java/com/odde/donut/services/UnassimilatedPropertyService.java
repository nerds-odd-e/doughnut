package com.odde.donut.services;

import com.odde.donut.algorithms.PropertyKeyNaming;
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
            user.getId(), user.getOwnership().getId()));
  }

  public Stream<AssimilationUnit> streamUnassimilatedPropertiesForSubscription(
      Subscription subscription) {
    return streamAssimilable(
        notePropertyIndexRepository.streamUnassimilatedPropertiesForNotebook(
            subscription.getUser().getId(), subscription.getNotebook().getId()));
  }

  private static int countAssimilable(Stream<AssimilationUnit> unfiltered) {
    try (Stream<AssimilationUnit> stream = streamAssimilable(unfiltered)) {
      return (int) stream.count();
    }
  }

  private static Stream<AssimilationUnit> streamAssimilable(Stream<AssimilationUnit> unfiltered) {
    return unfiltered.filter(
        unit -> !PropertyKeyNaming.isReservedStructuralKey(unit.propertyKey()));
  }
}

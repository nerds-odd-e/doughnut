package com.odde.doughnut.services;

import com.odde.doughnut.entities.NotePropertyIndex;
import com.odde.doughnut.entities.Subscription;
import com.odde.doughnut.entities.User;
import com.odde.doughnut.entities.repositories.NotePropertyIndexRepository;
import java.util.stream.Stream;
import org.springframework.stereotype.Service;

@Service
public class UnassimilatedPropertyService {

  private final NotePropertyIndexRepository notePropertyIndexRepository;

  public UnassimilatedPropertyService(NotePropertyIndexRepository notePropertyIndexRepository) {
    this.notePropertyIndexRepository = notePropertyIndexRepository;
  }

  public int countUnassimilatedPropertiesForUser(User user) {
    return notePropertyIndexRepository.countUnassimilatedPropertiesForOwnership(
        user.getId(), user.getOwnership().getId());
  }

  public int countUnassimilatedPropertiesForSubscription(Subscription subscription) {
    return notePropertyIndexRepository.countUnassimilatedPropertiesForNotebook(
        subscription.getUser().getId(), subscription.getNotebook().getId());
  }

  public Stream<AssimilationUnit> streamUnassimilatedPropertiesForUser(User user) {
    return notePropertyIndexRepository
        .streamUnassimilatedPropertiesForOwnership(user.getId(), user.getOwnership().getId())
        .map(UnassimilatedPropertyService::toPropertyUnit);
  }

  public Stream<AssimilationUnit> streamUnassimilatedPropertiesForSubscription(
      Subscription subscription) {
    return notePropertyIndexRepository
        .streamUnassimilatedPropertiesForNotebook(
            subscription.getUser().getId(), subscription.getNotebook().getId())
        .map(UnassimilatedPropertyService::toPropertyUnit);
  }

  private static AssimilationUnit toPropertyUnit(NotePropertyIndex index) {
    return AssimilationUnit.forProperty(index.getNote(), index.getPropertyKey());
  }
}

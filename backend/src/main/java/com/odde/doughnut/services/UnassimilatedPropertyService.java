package com.odde.doughnut.services;

import com.odde.doughnut.entities.NotePropertyIndex;
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
    return notePropertyIndexRepository.countUntrackedPropertiesForOwnership(
        user.getId(), user.getOwnership().getId());
  }

  public int countUnassimilatedPropertiesForNotebook(User user, Integer notebookId) {
    return notePropertyIndexRepository.countUntrackedPropertiesForNotebook(
        user.getId(), notebookId);
  }

  public Stream<AssimilationUnit> streamUnassimilatedPropertiesForUser(User user) {
    return notePropertyIndexRepository
        .streamUntrackedPropertiesForOwnership(user.getId(), user.getOwnership().getId())
        .map(UnassimilatedPropertyService::toPropertyUnit);
  }

  public Stream<AssimilationUnit> streamUnassimilatedPropertiesForNotebook(
      User user, Integer notebookId) {
    return notePropertyIndexRepository
        .streamUntrackedPropertiesForNotebook(user.getId(), notebookId)
        .map(UnassimilatedPropertyService::toPropertyUnit);
  }

  private static AssimilationUnit toPropertyUnit(NotePropertyIndex index) {
    return AssimilationUnit.forProperty(index.getNote(), index.getPropertyKey());
  }
}

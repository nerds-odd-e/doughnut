package com.odde.donut.services;

import com.odde.donut.entities.Subscription;
import com.odde.donut.entities.repositories.NoteRepository;
import java.util.List;
import java.util.stream.Stream;
import org.springframework.stereotype.Service;

@Service
public class SubscriptionService {
  private final NoteRepository noteRepository;

  public SubscriptionService(NoteRepository noteRepository) {
    this.noteRepository = noteRepository;
  }

  public int getUnassimilatedNoteCount(Subscription subscription) {
    return noteRepository.countUnassimilatedByAncestor(
        subscription.getUser().getId(), subscription.getNotebook().getId());
  }

  public Stream<AssimilationUnit> getUnassimilatedNotes(Subscription subscription) {
    return noteRepository.findUnassimilatedByAncestor(
        subscription.getUser().getId(), subscription.getNotebook().getId());
  }

  public int remainingDailyAssimilationTarget(Subscription subscription, List<Integer> noteIds) {
    int count =
        noteRepository.countByAncestorAndInTheList(subscription.getNotebook().getId(), noteIds);
    return Math.max(0, subscription.getDailyTargetOfNewNotes() - count);
  }
}

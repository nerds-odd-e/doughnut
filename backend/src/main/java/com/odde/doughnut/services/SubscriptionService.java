package com.odde.doughnut.services;

import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.Subscription;
import com.odde.doughnut.entities.repositories.NoteReviewRepository;
import java.util.List;
import java.util.stream.Stream;
import org.springframework.stereotype.Service;

@Service
public class SubscriptionService {
  private final NoteReviewRepository noteReviewRepository;

  public SubscriptionService(NoteReviewRepository noteReviewRepository) {
    this.noteReviewRepository = noteReviewRepository;
  }

  public int getUnassimilatedNoteCount(Subscription subscription) {
    return noteReviewRepository.countByAncestorWhereThereIsNoMemoryTracker(
        subscription.getUser().getId(), subscription.getNotebook().getId());
  }

  public Stream<Note> getUnassimilatedNotes(Subscription subscription) {
    return noteReviewRepository.findByAncestorWhereThereIsNoMemoryTracker(
        subscription.getUser().getId(), subscription.getNotebook().getId());
  }

  public int needToLearnCountToday(Subscription subscription, List<Integer> noteIds) {
    int count =
        noteReviewRepository.countByAncestorAndInTheList(
            subscription.getNotebook().getId(), noteIds);
    return Math.max(0, subscription.getDailyTargetOfNewNotes() - count);
  }
}

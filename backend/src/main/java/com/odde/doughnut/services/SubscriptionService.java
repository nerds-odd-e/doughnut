package com.odde.doughnut.services;

import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.Subscription;
import com.odde.doughnut.entities.repositories.NoteRepository;
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
    return noteRepository.countByAncestorWhereThereIsNoMemoryTracker(
        subscription.getUser().getId(), subscription.getNotebook().getId());
  }

  public Stream<Note> getUnassimilatedNotes(Subscription subscription) {
    return noteRepository.findByAncestorWhereThereIsNoMemoryTracker(
        subscription.getUser().getId(), subscription.getNotebook().getId());
  }

  public int needToLearnCountToday(Subscription subscription, List<Integer> noteIds) {
    int count =
        noteRepository.countByAncestorAndInTheList(subscription.getNotebook().getId(), noteIds);
    return Math.max(0, subscription.getDailyTargetOfNewNotes() - count);
  }
}

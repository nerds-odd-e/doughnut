package com.odde.doughnut.models;

import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.Subscription;
import com.odde.doughnut.factoryServices.ModelFactoryService;
import java.util.List;
import java.util.stream.Stream;

public class SubscriptionModel implements ReviewScope {
  protected final Subscription entity;
  protected final ModelFactoryService modelFactoryService;

  public SubscriptionModel(Subscription sub, ModelFactoryService modelFactoryService) {
    this.entity = sub;
    this.modelFactoryService = modelFactoryService;
  }

  public int getUnassimilatedNoteCount() {
    return modelFactoryService.noteReviewRepository.countByAncestorWhereThereIsNoMemoryTracker(
        entity.getUser().getId(), entity.getNotebook().getId());
  }

  @Override
  public Stream<Note> getUnassimilatedNotes() {
    return modelFactoryService.noteReviewRepository.findByAncestorWhereThereIsNoMemoryTracker(
        entity.getUser().getId(), entity.getNotebook().getId());
  }

  public int needToLearnCountToday(List<Integer> noteIds) {
    int count =
        modelFactoryService.noteReviewRepository.countByAncestorAndInTheList(
            entity.getNotebook().getId(), noteIds);
    return Math.max(0, entity.getDailyTargetOfNewNotes() - count);
  }
}

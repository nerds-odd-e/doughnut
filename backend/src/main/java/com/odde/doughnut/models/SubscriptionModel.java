package com.odde.doughnut.models;

import com.odde.doughnut.entities.Link;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.Subscription;
import com.odde.doughnut.factoryServices.ModelFactoryService;
import java.util.List;

public class SubscriptionModel implements ReviewScope {
  protected final Subscription entity;
  protected final ModelFactoryService modelFactoryService;

  public SubscriptionModel(Subscription sub, ModelFactoryService modelFactoryService) {
    this.entity = sub;
    this.modelFactoryService = modelFactoryService;
  }

  @Override
  public List<Note> getNotesHaveNotBeenReviewedAtAll() {
    return modelFactoryService.noteRepository.findByAncestorWhereThereIsNoReviewPoint(
        entity.getUser(), entity.getHeadNote());
  }

  @Override
  public int getNotesHaveNotBeenReviewedAtAllCount() {
    return modelFactoryService.noteRepository.countByAncestorWhereThereIsNoReviewPoint(
        entity.getUser(), entity.getHeadNote());
  }

  @Override
  public List<Link> getLinksHaveNotBeenReviewedAtAll() {
    return modelFactoryService.linkRepository.findByAncestorWhereThereIsNoReviewPoint(
        entity.getUser(), entity.getHeadNote());
  }

  @Override
  public int getLinksHaveNotBeenReviewedAtAllCount() {
    return modelFactoryService.linkRepository.countByAncestorWhereThereIsNoReviewPoint(
        entity.getUser(), entity.getHeadNote());
  }

  public int needToLearnCountToday(List<Integer> noteIds) {
    int count =
        modelFactoryService.noteRepository.countByAncestorAndInTheList(
            entity.getHeadNote(), noteIds);
    return entity.getDailyTargetOfNewNotes() - count;
  }
}

package com.odde.doughnut.models;

import com.odde.doughnut.entities.Subscription;
import com.odde.doughnut.entities.Thing;
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

  @Override
  public int getThingsHaveNotBeenReviewedAtAllCount() {
    return modelFactoryService.thingRepository.countByAncestorWhereThereIsNoReviewPoint(
        entity.getUser().getId(), entity.getHeadNote().getId());
  }

  @Override
  public Stream<Thing> getThingHaveNotBeenReviewedAtAll() {
    return modelFactoryService.thingRepository.findByAncestorWhereThereIsNoReviewPoint(
        entity.getUser().getId(), entity.getHeadNote().getId());
  }

  public int needToLearnCountToday(List<Integer> thingIds) {
    int count =
        modelFactoryService.thingRepository.countByAncestorAndInTheList(
            entity.getHeadNote().getId(), thingIds);
    return Math.max(0, entity.getDailyTargetOfNewNotes() - count);
  }
}

package com.odde.doughnut.services;

import com.odde.doughnut.entities.Subscription;
import com.odde.doughnut.entities.User;
import java.util.stream.Stream;
import org.springframework.stereotype.Service;

@Service
public class UnassimilatedPropertyUnitSource implements AssimilationUnitSource {
  private final UnassimilatedPropertyService unassimilatedPropertyService;

  public UnassimilatedPropertyUnitSource(
      UnassimilatedPropertyService unassimilatedPropertyService) {
    this.unassimilatedPropertyService = unassimilatedPropertyService;
  }

  @Override
  public int countForUser(User user) {
    return unassimilatedPropertyService.countUnassimilatedPropertiesForUser(user);
  }

  @Override
  public Stream<AssimilationUnit> streamForUser(User user) {
    return unassimilatedPropertyService.streamUnassimilatedPropertiesForUser(user);
  }

  @Override
  public int countForSubscription(Subscription subscription) {
    return unassimilatedPropertyService.countUnassimilatedPropertiesForSubscription(subscription);
  }

  @Override
  public Stream<AssimilationUnit> streamForSubscription(Subscription subscription) {
    return unassimilatedPropertyService.streamUnassimilatedPropertiesForSubscription(subscription);
  }
}

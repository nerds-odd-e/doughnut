package com.odde.doughnut.services;

import com.odde.doughnut.entities.Subscription;
import com.odde.doughnut.entities.User;
import java.util.stream.Stream;
import org.springframework.stereotype.Service;

@Service
public class UnassimilatedNoteUnitSource implements AssimilationUnitSource {
  private final UserService userService;
  private final SubscriptionService subscriptionService;

  public UnassimilatedNoteUnitSource(
      UserService userService, SubscriptionService subscriptionService) {
    this.userService = userService;
    this.subscriptionService = subscriptionService;
  }

  @Override
  public int countForUser(User user) {
    return userService.getUnassimilatedNoteCount(user);
  }

  @Override
  public Stream<AssimilationUnit> streamForUser(User user) {
    return userService.getUnassimilatedNotes(user).map(AssimilationUnit::forNote);
  }

  @Override
  public int countForSubscription(Subscription subscription) {
    return subscriptionService.getUnassimilatedNoteCount(subscription);
  }

  @Override
  public Stream<AssimilationUnit> streamForSubscription(Subscription subscription) {
    return subscriptionService.getUnassimilatedNotes(subscription).map(AssimilationUnit::forNote);
  }
}

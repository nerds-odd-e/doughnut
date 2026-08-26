package com.odde.donut.services;

import com.odde.donut.entities.Subscription;
import com.odde.donut.entities.User;
import java.util.stream.Stream;

public interface AssimilationUnitSource {
  int countForUser(User user);

  Stream<AssimilationUnit> streamForUser(User user);

  int countForSubscription(Subscription subscription);

  Stream<AssimilationUnit> streamForSubscription(Subscription subscription);
}

package com.odde.doughnut.services;

import com.odde.doughnut.entities.Subscription;
import com.odde.doughnut.entities.User;
import java.util.stream.Stream;

public interface AssimilationUnitSource {
  int countForUser(User user);

  Stream<AssimilationUnit> streamForUser(User user);

  int countForSubscription(Subscription subscription);

  Stream<AssimilationUnit> streamForSubscription(Subscription subscription);
}

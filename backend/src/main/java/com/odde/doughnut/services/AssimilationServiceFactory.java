package com.odde.doughnut.services;

import com.odde.doughnut.entities.User;
import java.sql.Timestamp;
import java.time.ZoneId;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public final class AssimilationServiceFactory {
  private final UserService userService;
  private final SubscriptionService subscriptionService;
  private final List<AssimilationUnitSource> unitSources;

  @Autowired
  public AssimilationServiceFactory(
      UserService userService,
      SubscriptionService subscriptionService,
      List<AssimilationUnitSource> unitSources) {
    this.userService = userService;
    this.subscriptionService = subscriptionService;
    this.unitSources = unitSources;
  }

  public AssimilationService create(User user, Timestamp currentUTCTimestamp, ZoneId timeZone) {
    return new AssimilationService(
        user, userService, subscriptionService, unitSources, currentUTCTimestamp, timeZone);
  }
}

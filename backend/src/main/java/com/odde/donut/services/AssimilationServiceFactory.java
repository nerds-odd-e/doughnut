package com.odde.donut.services;

import com.odde.donut.entities.User;
import java.sql.Timestamp;
import java.time.ZoneId;
import org.springframework.stereotype.Service;

@Service
public final class AssimilationServiceFactory {
  private final UserService userService;
  private final SubscriptionService subscriptionService;
  private final UnassimilatedPropertyService unassimilatedPropertyService;

  public AssimilationServiceFactory(
      UserService userService,
      SubscriptionService subscriptionService,
      UnassimilatedPropertyService unassimilatedPropertyService) {
    this.userService = userService;
    this.subscriptionService = subscriptionService;
    this.unassimilatedPropertyService = unassimilatedPropertyService;
  }

  public AssimilationService create(User user, Timestamp currentUTCTimestamp, ZoneId timeZone) {
    return new AssimilationService(
        user,
        userService,
        subscriptionService,
        unassimilatedPropertyService,
        currentUTCTimestamp,
        timeZone);
  }
}

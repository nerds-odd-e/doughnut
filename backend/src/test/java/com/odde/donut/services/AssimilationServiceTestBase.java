package com.odde.donut.services;

import com.odde.donut.entities.Note;
import com.odde.donut.entities.User;
import com.odde.donut.testability.SpringTestBase;
import java.sql.Timestamp;
import java.time.ZoneId;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;

abstract class AssimilationServiceTestBase extends SpringTestBase {
  @Autowired SubscriptionService subscriptionService;
  @Autowired UserService userService;
  @Autowired AssimilationServiceFactory assimilationServiceFactory;

  User user;
  User anotherUser;
  Timestamp day1;
  AssimilationService assimilationService;

  @BeforeEach
  void baseSetup() {
    user = makeMe.aUser().please();
    anotherUser = makeMe.aUser().please();
    day1 = makeMe.aTimestamp().of(1, 8).fromShanghai().please();
    assimilationService = assimilationServiceFor(user, day1);
  }

  AssimilationService assimilationServiceFor(User forUser, Timestamp at) {
    return assimilationServiceFactory.create(forUser, at, ZoneId.of("Asia/Shanghai"));
  }

  Note getNextNoteToAssimilate(AssimilationService service) {
    return service.getNextNoteToAssimilate().orElse(null);
  }
}

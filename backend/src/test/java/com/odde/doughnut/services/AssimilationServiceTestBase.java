package com.odde.doughnut.services;

import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.User;
import com.odde.doughnut.testability.MakeMe;
import java.sql.Timestamp;
import java.time.ZoneId;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
abstract class AssimilationServiceTestBase {
  @Autowired MakeMe makeMe;
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

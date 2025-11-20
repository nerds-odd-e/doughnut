package com.odde.doughnut.services;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import com.odde.doughnut.controllers.dto.InitialInfo;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.User;
import com.odde.doughnut.factoryServices.EntityPersister;
import com.odde.doughnut.testability.MakeMe;
import java.sql.Timestamp;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class MemoryTrackerServiceTest {
  @Autowired MakeMe makeMe;
  @Autowired EntityPersister entityPersister;
  @Autowired UserService userService;
  User user;
  Timestamp day1;
  MemoryTrackerService memoryTrackerService;

  @BeforeEach
  void setup() {
    user = makeMe.aUser().please();
    day1 = makeMe.aTimestamp().of(1, 8).fromShanghai().please();
    memoryTrackerService = new MemoryTrackerService(entityPersister, userService);
  }

  @Nested
  class Assimilating {

    @Test
    void assimilatingShouldSetBothInitialAndLastReviewAt() {
      Note note = makeMe.aNote().creatorAndOwner(user).please();
      InitialInfo initialInfo = new InitialInfo();
      initialInfo.noteId = note.getId();
      initialInfo.skipMemoryTracking = false;

      var memoryTrackers = memoryTrackerService.assimilate(initialInfo, user, day1);

      assertThat(memoryTrackers.get(0).getAssimilatedAt(), equalTo(day1));
      assertThat(memoryTrackers.get(0).getLastRecalledAt(), equalTo(day1));
    }
  }
}

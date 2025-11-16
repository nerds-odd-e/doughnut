package com.odde.doughnut.services;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import com.odde.doughnut.controllers.dto.InitialInfo;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.factoryServices.ModelFactoryService;
import com.odde.doughnut.models.UserModel;
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
  @Autowired ModelFactoryService modelFactoryService;
  UserModel userModel;
  Timestamp day1;
  MemoryTrackerService memoryTrackerService;

  @BeforeEach
  void setup() {
    userModel = makeMe.aUser().toModelPlease();
    day1 = makeMe.aTimestamp().of(1, 8).fromShanghai().please();
    memoryTrackerService =
        new MemoryTrackerService(modelFactoryService, makeMe.getTimestampService());
  }

  @Nested
  class Assimilating {

    @Test
    void assimilatingShouldSetBothInitialAndLastReviewAt() {
      Note note = makeMe.aNote().creatorAndOwner(userModel).please();
      InitialInfo initialInfo = new InitialInfo();
      initialInfo.noteId = note.getId();
      initialInfo.skipMemoryTracking = false;

      var memoryTrackers =
          memoryTrackerService.assimilate(initialInfo, userModel.getEntity(), day1);

      assertThat(memoryTrackers.get(0).getAssimilatedAt(), equalTo(day1));
      assertThat(memoryTrackers.get(0).getLastRecalledAt(), equalTo(day1));
    }
  }
}

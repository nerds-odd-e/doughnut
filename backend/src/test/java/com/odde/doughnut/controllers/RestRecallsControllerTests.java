package com.odde.doughnut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.*;

import com.odde.doughnut.controllers.currentUser.CurrentUser;
import com.odde.doughnut.controllers.dto.DueMemoryTrackers;
import com.odde.doughnut.controllers.dto.RecallStatus;
import com.odde.doughnut.entities.repositories.MemoryTrackerRepository;
import com.odde.doughnut.factoryServices.ModelFactoryService;
import com.odde.doughnut.services.AuthorizationService;
import com.odde.doughnut.services.UserService;
import com.odde.doughnut.testability.MakeMe;
import com.odde.doughnut.testability.TestabilitySettings;
import com.odde.doughnut.utils.TimestampOperations;
import java.sql.Timestamp;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.TestBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class RecallsControllerTests {
  @Autowired ModelFactoryService modelFactoryService;
  @Autowired AuthorizationService authorizationService;
  @Autowired MakeMe makeMe;
  @Autowired UserService userService;
  @Autowired MemoryTrackerRepository memoryTrackerRepository;
  @TestBean private CurrentUser currentUser = new CurrentUser(null);
  private final TestabilitySettings testabilitySettings = new TestabilitySettings();

  RecallsController controller;

  @BeforeEach
  void setup() {
    currentUser.setUser(makeMe.aUser().please());
    controller =
        new RecallsController(
            testabilitySettings, authorizationService, userService, memoryTrackerRepository);
  }

  RecallsController nullUserController() {
    currentUser.setUser(null);
    return new RecallsController(
        testabilitySettings, authorizationService, userService, memoryTrackerRepository);
  }

  @Nested
  class Overall {
    @Test
    void shouldNotBeAbleToSeeNoteIDontHaveAccessTo() {
      assertThrows(
          ResponseStatusException.class, () -> nullUserController().overview("Asia/Shanghai"));
    }

    @Test
    void shouldReturnCorrectRecallWindowEndTime() {
      Timestamp currentTime = makeMe.aTimestamp().of(0, 0).please();
      testabilitySettings.timeTravelTo(currentTime);

      RecallStatus status = controller.overview("Asia/Shanghai");

      assertEquals(
          TimestampOperations.addHoursToTimestamp(currentTime, 24), status.getRecallWindowEndAt());
    }
  }

  @Nested
  class Repeat {
    @Test
    void shouldNotBeAbleToSeeNoteIDontHaveAccessTo() {
      assertThrows(
          ResponseStatusException.class,
          () -> nullUserController().recalling("Asia/Shanghai", null));
    }

    @ParameterizedTest
    @CsvSource(
        useHeadersInDisplayName = true,
        delimiter = '|',
        textBlock =
            """
                next review at (in hours) | timezone     | expected count
                #------------------------------------------------------------
                -1                        | Asia/Tokyo   | 1
                0                         | Asia/Tokyo   | 1
                4                         | Asia/Tokyo   | 0
                4                         | Europe/Paris | 1
                12                        | Europe/Paris | 0
                """)
    void shouldGetMemoryTrackersBasedOnTimezone(
        int nextRecallAtHours, String timezone, int expectedCount) {
      Timestamp currentTime = makeMe.aTimestamp().of(0, 0).please();
      testabilitySettings.timeTravelTo(currentTime);
      makeMe
          .aMemoryTrackerBy(currentUser.getUser())
          .nextRecallAt(TimestampOperations.addHoursToTimestamp(currentTime, nextRecallAtHours))
          .please();
      DueMemoryTrackers dueMemoryTrackers = controller.recalling(timezone, null);
      assertThat(dueMemoryTrackers.getToRepeat(), hasSize(expectedCount));
    }

    @Test
    void shouldIncludeRecallStatusInDueMemoryTrackers() {
      Timestamp currentTime = makeMe.aTimestamp().of(0, 0).please();
      testabilitySettings.timeTravelTo(currentTime);
      makeMe.aMemoryTrackerBy(currentUser.getUser()).nextRecallAt(currentTime).please();

      DueMemoryTrackers dueMemoryTrackers = controller.recalling("Asia/Shanghai", 0);

      assertEquals(1, dueMemoryTrackers.toRepeatCount);
      assertEquals(1, dueMemoryTrackers.totalAssimilatedCount);
      assertEquals(
          TimestampOperations.addHoursToTimestamp(currentTime, 24),
          dueMemoryTrackers.getRecallWindowEndAt());
    }
  }
}

package com.odde.doughnut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.*;

import com.odde.doughnut.controllers.dto.InitialInfo;
import com.odde.doughnut.entities.*;
import com.odde.doughnut.factoryServices.ModelFactoryService;
import com.odde.doughnut.models.UserModel;
import com.odde.doughnut.testability.MakeMe;
import com.odde.doughnut.testability.TestabilitySettings;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class MemoryTrackerOnboardingControllerTests {
  @Autowired private ModelFactoryService modelFactoryService;
  @Autowired private MakeMe makeMe;
  private UserModel currentUser;
  private final TestabilitySettings testabilitySettings = new TestabilitySettings();

  private MemoryTrackerOnboardingController controller;

  @BeforeEach
  void setup() {
    currentUser = makeMe.aUser().toModelPlease();
    controller =
        new MemoryTrackerOnboardingController(
            modelFactoryService, currentUser, testabilitySettings);
  }

  MemoryTrackerOnboardingController nullUserController() {
    return new MemoryTrackerOnboardingController(
        modelFactoryService, makeMe.aNullUserModelPlease(), testabilitySettings);
  }

  @Nested
  class Onboarding {
    @Test
    void onboarding() {
      Note n = makeMe.aNote().creatorAndOwner(currentUser).please();
      assertThat(n.getId(), notNullValue());
      List<Note> memoryTrackerWithRecallSettings = controller.onboarding("Asia/Shanghai");
      assertThat(memoryTrackerWithRecallSettings, hasSize(1));
    }

    @Test
    void notLoggedIn() {
      assertThrows(
          ResponseStatusException.class, () -> nullUserController().onboarding("Asia/Shanghai"));
    }
  }

  @Nested
  class CreateInitialReviewPoint {
    @Test
    void create() {
      InitialInfo info = new InitialInfo();
      assertThrows(ResponseStatusException.class, () -> nullUserController().onboard(info));
    }
  }
}

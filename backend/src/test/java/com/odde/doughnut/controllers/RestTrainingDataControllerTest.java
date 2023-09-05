package com.odde.doughnut.controllers;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.odde.doughnut.exceptions.UnexpectedNoAccessRightException;
import com.odde.doughnut.factoryServices.ModelFactoryService;
import com.odde.doughnut.models.UserModel;
import com.odde.doughnut.testability.MakeMe;
import com.odde.doughnut.testability.TestabilitySettings;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = {"classpath:repository.xml"})
@Transactional
class RestTrainingDataControllerTest {
  @Autowired ModelFactoryService modelFactoryService;

  @Autowired MakeMe makeMe;
  private UserModel userModel;
  RestTrainingDataController controller;
  private TestabilitySettings testabilitySettings = new TestabilitySettings();

  @BeforeEach
  void setup() {
    userModel = makeMe.aUser().toModelPlease();
    controller =
        new RestTrainingDataController(modelFactoryService, userModel, testabilitySettings);
  }

  @Nested
  class getGoodTrainingData {
    @Test
    void itShouldNotAllowNonMemberToSeeTrainingData() {
      controller =
          new RestTrainingDataController(
              modelFactoryService, makeMe.aNullUserModel(), testabilitySettings);
      assertThrows(
          ResponseStatusException.class,
          () -> {
            controller.getGoodTrainingData();
          });
    }

    @Test
    void shouldReturnTrainingDataIfHavingReadingAuth() throws UnexpectedNoAccessRightException {
      String goodTrainingData = controller.getGoodTrainingData();
      assertTrue(goodTrainingData.length() > 0);
    }
  }
}

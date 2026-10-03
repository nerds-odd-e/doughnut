package com.odde.donut.controllers;

import static org.junit.jupiter.api.Assertions.assertThrows;

import com.odde.donut.exceptions.OpenAiNotAvailableException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class AiControllerTest extends ControllerTestBase {
  @Autowired AiController controller;

  @BeforeEach
  void setup() {
    currentUser.setUser(makeMe.aUser().please());
  }

  @Nested
  class GetModelVersions {

    @Test
    void shouldThrowWhenOpenAiNotAvailable() {
      testabilitySettings.setOpenAiTokenOverride("");
      assertThrows(OpenAiNotAvailableException.class, () -> controller.getAvailableGptModels());
    }
  }
}

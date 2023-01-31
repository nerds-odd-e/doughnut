package com.odde.doughnut.controllers;

import static org.springframework.test.util.AssertionErrors.assertNotNull;
import static org.springframework.test.util.AssertionErrors.assertTrue;

import com.odde.doughnut.exceptions.UnexpectedNoAccessRightException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.validation.BindException;

public class RestOpenAiControllerTest {
  RestOpenAiController controller;

  @BeforeEach
  void setup() {
    controller = new RestOpenAiController();
  }

  @Test
  void whenUserCallsOpenAiWithTitle() throws UnexpectedNoAccessRightException, BindException {
    String response = controller.getOpenAiResponse("Elon Musk");
    assertNotNull("Response should not be null", response);
    assertTrue("Message should not be empty", response.length() > 0);
  }
}

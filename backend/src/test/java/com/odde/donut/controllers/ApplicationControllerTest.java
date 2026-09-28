package com.odde.donut.controllers;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

class ApplicationControllerTest extends ControllerTestBase {
  @Autowired private MockMvc mockMvc;

  @Test
  void usersIdentifyIsNotMapped() throws Exception {
    mockMvc.perform(get("/users/identify")).andExpect(status().isNotFound());
  }
}

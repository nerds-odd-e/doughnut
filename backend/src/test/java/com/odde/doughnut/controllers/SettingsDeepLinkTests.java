package com.odde.doughnut.controllers;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.forwardedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SettingsDeepLinkTests {
  @Autowired private MockMvc mockMvc;

  @Test
  void settingsDeepLinkIsServedBySpaShell() throws Exception {
    mockMvc
        .perform(get("/settings"))
        .andExpect(status().isOk())
        .andExpect(forwardedUrl("/index.html"));
  }

  @Test
  void settingsNestedDeepLinkIsServedBySpaShell() throws Exception {
    mockMvc
        .perform(get("/settings/recall-stats"))
        .andExpect(status().isOk())
        .andExpect(forwardedUrl("/index.html"));
  }
}

package com.odde.donut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.matchesPattern;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.info.BuildProperties;
import org.springframework.test.web.servlet.MockMvc;

class HealthCheckControllerTest extends ControllerTestBase {
  @Autowired MockMvc mockMvc;
  @Autowired BuildProperties buildProperties;

  @Test
  void startedApplicationAnswersAnonymousPingWithProfileAndDeployedCommit() throws Exception {
    String commit = buildProperties.get("commit");
    assertThat(commit, matchesPattern("[0-9a-f]{40}"));
    mockMvc
        .perform(get("/api/healthcheck"))
        .andExpect(status().isOk())
        .andExpect(content().string("OK. Active Profile: test. Commit: " + commit));
  }
}

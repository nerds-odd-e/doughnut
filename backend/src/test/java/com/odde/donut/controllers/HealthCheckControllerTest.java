package com.odde.donut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.matchesPattern;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.info.BuildProperties;

class HealthCheckControllerTest extends ControllerTestBase {
  @Autowired HealthCheckController controller;
  @Autowired BuildProperties buildProperties;

  @Test
  void pingIncludesDeployedCommit() {
    String commit = buildProperties.get("commit");
    assertThat(commit, matchesPattern("[0-9a-f]{40}"));
    assertThat(controller.ping(), equalTo("OK. Active Profile: test. Commit: " + commit));
  }
}

package com.odde.donut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import java.util.Properties;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.info.BuildProperties;
import org.springframework.test.context.bean.override.convention.TestBean;

class HealthCheckControllerTest extends ControllerTestBase {
  static final String FIXTURE_COMMIT = "fixture-commit-sha";

  @Autowired HealthCheckController controller;

  @TestBean BuildProperties buildProperties;

  static BuildProperties buildProperties() {
    Properties properties = new Properties();
    properties.setProperty("commit", FIXTURE_COMMIT);
    return new BuildProperties(properties);
  }

  @Test
  void pingIncludesDeployedCommit() {
    assertThat(controller.ping(), equalTo("OK. Active Profile: test. Commit: " + FIXTURE_COMMIT));
  }
}

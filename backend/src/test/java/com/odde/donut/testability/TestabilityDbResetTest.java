package com.odde.donut.testability;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import com.odde.donut.services.notebookGit.NotebookGitCutoverService;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

/**
 * Calls the real E2E reset, whose {@code TRUNCATE}s commit implicitly: this test really empties the
 * shared unit-test database. That is safe only while backend tests run sequentially in one JVM and
 * no test depends on another test's committed data; enabling parallel test execution must isolate
 * this class first.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class TestabilityDbResetTest {

  @Autowired TestabilityRestController testabilityRestController;
  @Autowired NotebookGitCutoverService notebookGitCutoverService;
  @Autowired MakeMe makeMe;
  @Autowired JdbcTemplate jdbcTemplate;

  @Test
  void resetLeavesNoNativeGitObjects() {
    notebookGitCutoverService.createBindingForNotebook(makeMe.aNotebook().please(), Instant.now());

    testabilityRestController.resetDBAndTestabilitySettings();

    assertThat(
        jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM notebook_git_accepted_object", Long.class),
        equalTo(0L));
  }
}

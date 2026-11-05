Feature: Cypress retry
  As a developer, I want ordinary Cypress runMode retries enabled,
  so that a scenario that fails once can still succeed within the same run.

  Scenario: Retry succeeds
    Given a retry succeeds

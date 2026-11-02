Feature: Cypress retry
  As a developer, I want ordinary Cypress runMode retries enabled,
  so that a scenario that fails once can still succeed within the same run.

  # Deliberate proof generation retains both attempts of the successful retry.

  Scenario: Retry succeeds
    Given a retry succeeds

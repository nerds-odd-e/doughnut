Feature: Failure report
  As a developer, I want exceptions recorded in the failure report so I can investigate the root cause.

  Scenario: Exception appears in the failure report
    When Someone triggered an exception
    Then an admin should see "RuntimeException" in the failure report

  Scenario: Admin clears a failure report item
    Given Someone triggered an exception
    And an admin is viewing the failure report
    When I clear the selected failure report item
    Then the failure report should be empty

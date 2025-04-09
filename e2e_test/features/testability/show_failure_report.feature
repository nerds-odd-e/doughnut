Feature: failure report
  As a developer, I want to see the failure report when there is an exception,
  so that I can investigate the root cause of the exception.

  Scenario: exception should be reported
    When Someone triggered an exception
    Then an admin should see "RuntimeException" in the failure report
    And each item in the failure report should have a checkbox
    When I check the checkbox for the failure report item
    And I click the delete button
    Then the failure report should be empty

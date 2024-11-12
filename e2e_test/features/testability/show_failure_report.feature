Feature: failure report

  Scenario: add failure
    When Someone triggered an exception
    Then an admin should see "RuntimeException" in the failure report

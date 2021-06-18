Feature: failure report

  Scenario: add failure
    When Someone triggered an exception
    And I've logged in as "developer"
    Then I should see "RuntimeException" in the failure report


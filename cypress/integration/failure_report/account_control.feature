Feature: Developer Only Use

  @ignore
  Scenario: Failure report browsing as non-user
    Given I haven't login
    When I access "failure_report"
    Then I should see "Please sign in"

  @ignore
  Scenario: Failure report browsing as developer-user
    Given I've logged in as "developer"
    When I access "failure_report"
    Then I should see "FailureReport"

  @ignore
  Scenario: Top browsing as developer-user
    Given I've logged in as "developer"
    When I access "/"
    Then I should see "Failure Reports"

  @ignore
  Scenario: Top browsing as non_developer-user
    Given I've logged in as "non_developer"
    When I access "/"
    Then I should see "Failure Reportsss"
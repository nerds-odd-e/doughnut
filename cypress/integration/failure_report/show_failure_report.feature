Feature: failure report display
  As a learner, I want to display failure reports.

  Background:
    Given I've logged in as an existing user

  Scenario: show failure report list page
    Given I've failure report
    When I open the "failure-report-list" set address bar
    Then I should see "Failure report list" in the page

  Scenario: show failure report page
    Given I've failure report
    When I open the "failure-report-list/show/1" set address bar
    Then I should see "Failure report" in the page

  Scenario: not add failure
    Given Someone open the "bad-request-page" set address bar
    Given Login state is "Developer"
    When I open the "failure-report-list" set address bar
    Then I should not see failure report in the page

  Scenario: add failure
    Given Someone open the "testability/exception" set address bar
    Given Login state is "Developer"
    When I open the "failure-report-list" set address bar
    Then I should see "RuntimeException" in the page
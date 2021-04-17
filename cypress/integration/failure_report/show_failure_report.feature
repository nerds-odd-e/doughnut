Feature: failure report

  Scenario: add failure
    Given Someone triggered an exception
    Given I've logged in as "developer"
    When I open the "failure-report-list" set address bar
    Then I should see "RuntimeException" in the page

  Scenario: add issue and failure-report
    Given There are no open issues on github
    Given Someone triggered an exception
    Given I've logged in as "developer"
     When I click the Doughnut Failure Report link in Github issue
     Then I should see Exception in the page

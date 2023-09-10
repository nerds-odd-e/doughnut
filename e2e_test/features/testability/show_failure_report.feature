Feature: failure report

  Scenario: add failure
    When Someone triggered an exception
    Then an admin should see "RuntimeException" in the failure report

  @requiresDeveloperSecret @ignore
  Scenario: add issue and failure-report
    Given Use real github sandbox and there are no open issues on github
    When Someone triggered an exception
    Then I should see a new open issue on github

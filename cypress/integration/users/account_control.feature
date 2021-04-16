Feature: Access control

  Scenario Outline: Only developers can see failure report page
    Given I've logged in as "<user>"
    When I visit "<access page>" page
    Then The "<displayed page>" page is displayed
    Examples:
      | user          | access page       | displayed page    |
      | none          | FailureReportPage | LoginPage         |
      | developer     | FailureReportPage | FailureReportPage |
      | non_developer | FailureReportPage | ErrorPage         |


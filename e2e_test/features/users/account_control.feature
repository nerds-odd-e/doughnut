Feature: Access control

  Scenario Outline: Only Admins can see failure report page
    Given I am logged in as "<user>"
    When I visit "<access page>" page
    Then The "<displayed page>" page is displayed
    Examples:
      | user      | access page       | displayed page    |
      # | none      | FailureReportPage | LoginPage         |
      | admin     | FailureReportPage | FailureReportPage |
      | non_admin | FailureReportPage | ErrorPage         |


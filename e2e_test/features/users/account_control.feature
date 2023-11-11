Feature: Access control

  Scenario Outline: Only Admins can see failure report page
    Given I am logged in as "<user>"
    When I visit the falure reports on the admin page
    Then The "<displayed page>" page is displayed
    Examples:
      | user      | displayed page    |
      | none      | LoginPage         |
      | admin     | FailureReportPage |
      | non_admin | ErrorPage         |


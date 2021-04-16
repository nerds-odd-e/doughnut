Feature: Access control failure report
  
  Scenario Outline: Only developers can see failure report page
    Given I've failure report
    And Login state is "<login state>"
    When Access to "<access page>" page
    Then The "<displayed page>" page is displayed
    Examples:
      | login state  | access page             | displayed page          |
      | None         | FailureReportPage       | LoginPage               |
      | None         | FailureReportDetailPage | LoginPage               |
      | Developer    | FailureReportPage       | FailureReportPage       |
      | Developer    | FailureReportDetailPage | FailureReportDetailPage |
      | NonDeveloper | FailureReportPage       | ErrorPage               |
      | NonDeveloper | FailureReportDetailPage | ErrorPage               |

  Scenario Outline: Only developers can see failure report link
    Given I've failure report
    And Login state is "<login state>"
    When Access to top page
    Then The <target function> is "<expected>"
    Examples:
      | login state  | target function    | expected  |
      | Developer    | FailureReportsLink | visible   |
      | NonDeveloper | FailureReportsLink | invisible |


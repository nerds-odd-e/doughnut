Feature: Access control failure report

  Scenario Outline: Only available to developer users
    Given Login state is "<login state>"
    Given I've failure report
    When Access to failure report page
    Then The "<page>" page is displayed
    When Access to failure report detail page
    Then The "<detail page>" page is displayed
    When Access to top page
    Then Failure reports menu is "<displayed>" in the header
    Examples:
      | login state  | page              | detail page             | displayed    |
      | None         | LoginPage         | LoginPage               | NotDisplayed |
      | Developer    | FailureReportPage | FailureReportDetailPage | Displayed    |
      | NonDeveloper | ErrorPage         | ErrorPage               | NotDisplayed |

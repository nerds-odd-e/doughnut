Feature: Access control failure report

  Scenario Outline: Only available to developer users
    Given Login state is "<login state>"
    When Access to failure report page
    Then The "<page>" page is displayed
    When Access to top page
    Then Failure reports menu is "<displayed>" in the header
    Examples:
      | login state     | page              | displayed     |
      | None            | LoginPage         | NotDisplayed  |
      | Developer       | FailureReportPage | Displayed     |
      | NonDeveloper    | ErrorPage         | NotDisplayed  |

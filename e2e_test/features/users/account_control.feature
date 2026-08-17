Feature: Access control

  Scenario Outline: Only admins can open failure reports
    Given I have a session as "<user>"
    When I open the failure reports
    Then the failure reports access outcome is "<outcome>"
    Examples:
      | user      | outcome          |
      | none      | sign in          |
      | admin     | failure reports  |
      | non_admin | access denied    |

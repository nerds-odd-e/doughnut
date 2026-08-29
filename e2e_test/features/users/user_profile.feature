Feature: User profile

  Background:
    Given I am logged in as an existing user

  Scenario: Change display name
    When I change my display name to "Barbie"
    Then my display name "Barbie" is shown in the account menu

  Scenario Outline: Daily probe setting persists after reload
    Given Daily probe is <from>
    When I turn Daily probe <to>
    Then Daily probe is <to> after I reload my settings

    Examples:
      | from | to  |
      | off  | on  |
      | on   | off |

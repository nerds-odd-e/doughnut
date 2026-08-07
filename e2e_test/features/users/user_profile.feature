Feature: User profile

  Background:
    Given I am logged in as an existing user

  Scenario: Change display name
    When I change my display name to "Barbie"
    Then my display name "Barbie" is shown in the account menu

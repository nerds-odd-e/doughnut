Feature: user profile

  Background:
    Given I am logged in as an existing user

  Scenario: Edit user profile
    When I edit user profile
    And I change my name to "Barbie"
    Then I should see "Barbie" in the page


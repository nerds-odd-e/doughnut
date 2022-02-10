Feature: Logged in status

  Background:
    Given I've logged in as an existing user

  Scenario: logout
    When I logout via the UI
    Then I should be on the welcome page and asked to login

  Scenario: User logout because of session timeout
    Given there are some notes for the current user
      | title                | testingParent |
      | Shape                |               |
      | Triangle             | Shape         |
    And I open "Shape" note from top level
    When my session is logged out
    Then I should be asked to log in again when I click the link "Triangle"
    When I login as "old_learner" I should see "Triangle"

  Scenario: Session timeout out when post
    Given I added and learned one note "Fungible" on day 1
    When I am repeat-reviewing my old note on day 2
    And I should see the happy option
    And my session is logged out
    When I choose the happy option
    When I login as "old_learner" I should see "Fungible"
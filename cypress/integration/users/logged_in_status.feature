Feature: Logged in status

  Background:
    Given I've logged in as an existing user

  Scenario: Session logged out
    Given there are some notes for the current user
      | title                | testingParent |
      | Shape                |               |
      | Triangle             | Shape         |
    And I open "Shape" note from top level
    When my session is logged out
    Then I should be asked to log in again when I click the link "Triangle"
    And when I login as "old_learner" I should see "Triangle"


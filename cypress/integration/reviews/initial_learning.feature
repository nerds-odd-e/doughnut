Feature: Initial learning
  As a learner, I want to add note to my future review list.

  Background:
    Given I've logged in as an existing user
    And there are some notes for the current user
      | title    |
      | hard     |
      | easy     |

  Scenario: Update review setting
    When I am learning new note on day 1
    And I set the level of "hard" to be 2
    And I learned one note "easy" on day 1
    When I am learning new note on day 3


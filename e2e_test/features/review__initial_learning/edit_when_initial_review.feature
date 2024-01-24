Feature: Editing when initial review

  Background:
    Given I am logged in as an existing user
    And there are some notes for the current user:
      | topicConstructor |
      | hard             |
      | easy             |

  Scenario: Update note
    When I am learning new note on day 1
    Then I can change the topic "hard" to "harder"

  Scenario: Update review setting
    Given I am learning new note on day 1
    When I set the level of "hard" to be 2
    Then I learned one note "easy" on day 1
    And I learned one note "hard" on day 2



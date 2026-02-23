Feature: Editing when assimilating

  Background:
    Given I am logged in as an existing user
    And I have a notebook with the head note "hard"
    And I have a notebook with the head note "easy"

  Scenario: Update note
    When I am assimilating new note on day 1
    Then I can change the title "hard" to "harder"

  Scenario: Update recall setting
    Given I am assimilating new note on day 1
    When I set the level of "hard" to be 2
    Then I assimilated one note "easy" on day 1
    And I assimilated one note "hard" on day 2

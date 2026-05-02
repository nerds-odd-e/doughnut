Feature: Editing when assimilating

  Background:
    Given I am logged in as an existing user
    And I have a notebook "Hard deck" with a note "hard"
    And I have a notebook "Easy deck" with a note "easy"

  Scenario: Update note
    Given It's day 1
    When I am assimilating the note "hard"
    Then I can change the title "hard" to "harder"

  Scenario: Update recall setting
    Given It's day 1
    When I am assimilating the note "hard"
    When I set the level of "hard" to be 2
    Then I assimilated one note "easy" on day 1
    And I assimilated one note "hard" on day 2

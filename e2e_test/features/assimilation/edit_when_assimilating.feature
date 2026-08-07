Feature: Editing when assimilating

  Background:
    Given I am logged in as an existing user
    And I have a notebook "Hard deck" with a note "hard"
    And I have a notebook "Easy deck" with a note "easy"

  Scenario: Update note title while assimilating
    Given It's day 1
    When I am assimilating the note "hard"
    And I change the title from "hard" to "harder"
    Then the note title should be "harder"

  Scenario: Update recall level while assimilating
    Given It's day 1
    When I am assimilating the note "hard"
    And I set the level of "hard" to be 2
    Then the note "easy" was assimilated on day 1
    And the note "hard" was assimilated on day 2

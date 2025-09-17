@ignore
Feature: Multiple players

  @ignore
  Scenario: First player joins the game
    Given a new game
    When a player joins
    Then the player count is 1
    And their car is at step 0 with 0 damage
    When they play their turn in "Normal" mode and roll a 3
    Then their car moves to step 1 with 0 damage

  @ignore
  Scenario: Second player joins the game
    Given a new game with one player
    When a second player joins
    Then the player count is 2
    And their car is at step 0 with 0 damage
    When they play their turn in "Super" mode and roll a 3
    Then their car moves to step 3 with 1 damage


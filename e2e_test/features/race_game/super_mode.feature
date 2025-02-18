@ignore
Feature: Super Mode in Race Game

  @super_mode
  Scenario: move car when HP is max value
    Given the car at position 0
    And the car has HP 6
    When the player go super mode
    And the dice outcome is 6
    Then the car should move to position 6
    And the HP of car becomes 5

  @super_mode
  Scenario: move car when HP is not max
    Given the car at position 3
    And the car has HP 4
    When the player go super mode
    And the dice outcome is 3
    Then the car should move to position 4
    And the HP of car becomes 3

  @super_mode
  Scenario: car cannot move when HP is too low & dice is too small
    Given the car at position 4
    And the car has HP 1
    When the player go super mode
    And the dice outcome is 3
    Then the car should not move
    And the HP of car becomes 0

  @super_mode
  Scenario: car cannot move when the HP is 0
    Given the car has HP 0
    When the player go super mode
    Then the car should not move

  @super_mode
  Scenario: Reset the game
    Given the car at position 10
    And the car has HP 0
    When the player reset the game
    Then the car should move to position 0
    And the HP of car becomes 6

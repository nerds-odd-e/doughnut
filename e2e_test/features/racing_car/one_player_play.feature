Feature: one player play game

  Background:
    Given I am a player and in the game screen, round 0

  Scenario: Super mode roll shows correct result
  When I choose the super mode and I roll the dice
   Then the total steps shown should match the backend response
    And the total damage shown should match the backend response



  @ignore
  Scenario: Car cannot move when total damage exceeds 6
    Given I am on round 6 with 6 damage
    When I roll a dice and get 6
    Then the total damage should be 7
    And the the car moves 0 steps

  @ignore
  Scenario: Single player can play the normal mode
    When I choose to play the normal mode and roll the dice
    Then the total damage becomes 0
    Then the dice number has value in range 1-6
    And the car moves 1 steps or more
    When I choose to play the normal mode and roll the dice
    Then the total damage becomes 0
    Then the dice number has value in range 1-6
    And the car moves 2 steps or more


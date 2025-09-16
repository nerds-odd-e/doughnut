Feature: one player play game with super mode only

  Background:
    Given I am a player and in the game screen, round 0
  
  @ignore
  Scenario: Start play a new game
    When I am start the round 1 with the dice number roll equal 5
    Then The car moves 5 steps.
    And Total step equals 5
    And Total damage equals 1
    And The round number become 2
    When I am start the round 2 with the dice number roll equal 4
    And the current damage is 1
    Then The car moves 3 steps.
    And Total step equals 8
    And Total damage equals 2
    And The round number become 3
    When I am start the round 3 with the dice number roll equal 1
    And the current damage is 2
    Then The car don't moves.
    And Total step equals 8
    And Total damage equals 3
    And The round number become 4
    When I am start the round 4 with the dice number roll equal 6
    And the current damage is 3
    Then The car moves 3 steps.
    And Total step equals 11
    And Total damage equals 4
    And The round number become 5
  
  @ignore
  Scenario: playing game with total damaged is 6
    When I rolling a dice
    And The number equal 6
    Then the total damaged is 7
    And the car can't move


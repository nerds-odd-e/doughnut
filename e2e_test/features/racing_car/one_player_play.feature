Feature: one player play game

  Background:
    Given I am a player and in the game screen, round 0

# @ignore
  Scenario Outline: Single player can play the super mode
    When I choose the super mode and I roll the dice
    Then the dice number has value in range 1-6
    And the total damage should be <total_damage>
    # And the total steps should be <total_steps>
    # And the round number becomes <next_round>
    And the car moves random steps

    Examples:
      | round_number | steps_moved | total_steps | total_damage | next_round |
      | 1            | 5           | 5           | 1            | 2          |


  @ignore
  Scenario: Car cannot move when total damage exceeds 6
    Given I am on round 6 with 6 damage
    When I roll a dice and get 6
    Then the total damage should be 7
    And the the car moves 0 steps


  Scenario: Single player can play the normal mode
    When I choose to play the normal mode and roll the dice
    Then the total damage becomes 0
    Then the dice number has value in range 1-6
    And the car moves 1 or 2 steps

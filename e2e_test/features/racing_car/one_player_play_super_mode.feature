Feature: one player play game with super mode only

  Background:
    Given I am a player and in the game screen, round 0

  @ignore
  Scenario Outline: Player progresses through rounds
    Given I am on round <round_number> with <current_damage> damage
    When I roll the dice and get <dice_roll>
    Then the car moves <steps_moved> steps
    And the total steps should be <total_steps>
    And the total damage should be <total_damage>
    And the round number becomes <next_round>

    Examples:
      | round_number | current_damage | dice_roll | steps_moved | total_steps | total_damage | next_round |
      | 1            | 0              | 5         | 5           | 5           | 1            | 2          |
      | 2            | 1              | 4         | 3           | 8           | 2            | 3          |
      | 3            | 2              | 1         | 0           | 8           | 3            | 4          |
      | 4            | 3              | 6         | 3           | 11          | 4            | 5          |


  @ignore
  Scenario: Car cannot move when total damage exceeds 6
    Given the total damage is 6
    When I roll a dice
    And the dice roll result is 6
    Then the total damage becomes 7
    And the car cannot move


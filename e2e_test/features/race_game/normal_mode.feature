Feature: Normal Mode in Race Game

  @normal_mode
  Scenario: Go normal mode
    Given I am at the beginning of the race game
    When I choose to go normal for this round
    Then my car should move no further than 2 steps at round 1

  # @normal_mode
  # Scenario: Reset the game
  #   Given the car at position 21
  #   And the round count is 10
  #   When the player reset the game
  #   Then the car should move to position 0
  #   And the round count becomes 1

Feature: Normal Mode in Race Game

  @normal_mode
  Scenario: Go normal mode
    Given I am at the beginning of the race game
    When I choose to go normal for this round
    Then my car should move no further than 2 steps at round 1

  @normal_mode
  Scenario: Reset the game
    Given I am at the beginning of the race game
    When I choose to go normal for this round
    And I reset the game
    Then my car should at the beginning of the race game and the round count is 0


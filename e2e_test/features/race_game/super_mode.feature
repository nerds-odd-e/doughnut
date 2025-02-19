@ignore
@raceGame
Feature: Super Mode in Race Game

  @superMode
  Scenario: Go super mode
    Given I am at the beginning of the race game
    When I choose to go super for this round
    Then my car should move no further than 6 steps at round 1
    And my car HP should become 5
    When I choose to go super for this round
    Then my car should move no further than 5 steps at round 2
    And my car HP should become 4

  @superMode
  Scenario: Reset the game
    Given I am at the beginning of the race game
    When I choose to go super for this round
    And I reset the game
    Then my car should at the beginning of the race game and the round count is 0
    And my car HP should be 6
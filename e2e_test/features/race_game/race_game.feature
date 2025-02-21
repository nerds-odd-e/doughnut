@raceGame
Feature: Normal Mode in Race Game

  @focus
  Scenario: Join the game
    When I am at the beginning of the race game as "Rahul"
    Then I should see a car belongs to "Rahul"

  Scenario: Go normal mode
    Given I am at the beginning of the race game as "Rahul"
    When I choose to go normal for this round
    Then my car should move no further than 2 steps at round 1

  @ignore
  Scenario: Reset the Game
    Given I am at the beginning of the race game as "Rahul"
    When I choose to go normal for this round
    And I reset the game
    Then I should see my car at position 0
    And I should see round count is 0

  # Scenario: Go super mode
  #   Given I am at the beginning of the race game
  #   When I choose to go super for this round
  #   Then my car should move no further than 6 steps at round 1
  #   And my car HP should become 5
  #   When I choose to go super for this round
  #   Then my car should move no further than 5 steps at round 2
  #   And my car HP should become 4
  #   When I choose to go super for this round
  #   Then my car should move no further than 4 steps at round 3
  #   And my car HP should become 3
  #   When I choose to go super for this round
  #   Then my car should move no further than 3 steps at round 4
  #   And my car HP should become 2
  #   When I choose to go super for this round
  #   Then my car should move no further than 2 steps at round 5
  #   And my car HP should become 1
  #   When I choose to go super for this round
  #   Then my car should move no further than 1 steps at round 6
  #   And my car HP should become 0
  #   When I choose to go super for this round
  #   Then my car should no longer move

  # Scenario: Reset the game
  #   Given I am at the beginning of the race game
  #   When I choose to go super for this round
  #   And I reset the game
  #   Then my car should at the beginning of the race game and the round count is 0
  #   And my car HP should be 6
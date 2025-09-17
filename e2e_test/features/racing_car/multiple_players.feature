Feature: Multiple players

  Background:
    Given I am a player and in the game screen, round 0

  Scenario: Second player joins the game
    When a new player joins
    Then the player count is "1"
#    And their car is at position "0" with "0" damage


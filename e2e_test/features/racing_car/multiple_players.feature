Feature: Multiple players

  Background:
    Given I am a player and in the game screen, round 0

  Scenario: Second player joins the game
    When a new player joins
    Then the player count is "2"
#    And their car is at position "0" with "0" damage

  @ignore
  Scenario: a player joins the game and the number of players increase
    Given a player join
    When a player joins
    Then the number of players is 1

  @ignore
  Scenario: the game had one player and a new player joins the game and the number of players increases
    Given the number of players is 1
    When a new player joins
    Then the number of players are 2



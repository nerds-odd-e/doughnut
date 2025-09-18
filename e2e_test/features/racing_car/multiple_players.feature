Feature: Multiple players

  Scenario: First player joins the game
    When A new player joins the game
    Then The player count is "1"

  Scenario: Second player joins the game
    Given I am a player and in the game screen, round 0
    When A new player joins the game
    Then The player count is "2"
#    And their car is at position "0" with "0" damage

  Scenario: Multiple players join the game
    When A new player joins the game
    And A new player joins the game
    And A new player joins the game
    And A new player joins the game
    Then The player count is "4"

  @ignore
  Scenario: the game had one player and a new player joins the game and the number of players increases
    Given The number of players is 1
    When A new player joins the game
    Then The number of players are 2



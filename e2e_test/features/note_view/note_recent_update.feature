Feature: see recent note update
  As a learner, I want to see which of my notes are recently updated,
  so that I can focus on only recalling the newly updated notes.

  Background:
    Given I am logged in as an existing user
    And I let the server to time travel to 100 hours ago
    And I have a notebook with head note "World" and notes:
      | Title   | Parent Title | Details           |
      | Germany | World        |                   |
      | Japan   | World        |                   |
      | Berlin  | Germany      | Berlin has a wall |
      | Munich  | Germany      | Munich has beer   |
      | Italy   | World        |                   |
    And I let the server to time travel to 24 hours ago

  Scenario Outline: I should see the color of a newer note is fresher
    And I update note "Berlin" with details "<new details>"
    Then I should see "Berlin" is "<aging>" than "Japan"

    Examples:
      | new details       | aging     |
      | Berlin had a wall | newer     |
      | Berlin has a wall | not newer |

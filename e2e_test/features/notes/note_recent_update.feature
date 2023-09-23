Feature: see recent note update
  As a learner, I want to see which of my notes are recently updated,
  so that I can focus on only reviewing the newly updated notes.

  Background:
    Given I am logged in as an existing user
    And I let the server to time travel to 100 hours ago
    And there are some notes for the current user:
      | topic    | testingParent | details       |
      | NoteBook |               |             |
      | Note1    | NoteBook      |             |
      | Note2    | NoteBook      |             |
      | Note1.1  | Note1         | <p>note1.1</p> |
      | Note1.2  | Note1         | note1.2     |
      | Note3    | NoteBook      |             |
    And I let the server to time travel to 24 hours ago

  Scenario Outline: I should see the color of a newer note is fresher
    And I update note "Note1.1" with details "<new details>"
    Then I should see "Note1.1" is "<aging>" than "Note2"

    Examples:
      | new details       | aging     |
      | updated note1.1 | newer     |
      | note1.1         | not newer |

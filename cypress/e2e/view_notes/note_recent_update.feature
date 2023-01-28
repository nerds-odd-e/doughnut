Feature: see recent note update
  As a learner, I want to see which of my notes are recently updated,
  so that I can focus on only reviewing the newly updated notes.

  Background:
    Given I've logged in as an existing user
    And I let the server to time travel to 100 hours ago
    And there are some notes for the current user
      | title    | testingParent | description |
      | NoteBook |               |             |
      | Note1    | NoteBook      |             |
      | Note2    | NoteBook      |             |
      | Note1.1  | Note1         | note1.1     |
      | Note1.2  | Note1         | note1.2     |
      | Note3    | NoteBook      |             |
    And I let the server to time travel to 24 hours ago

  Scenario Outline: I should see new note banner on newly updated note
    And I update note "Note1.1" with description "<new description>"
    Then I should see "Note1.1" is "<aging>" than "Note2"

    Examples:
      | new description | aging     |
      | updated note1.1 | newer     |
      | note1.1         | not newer |

Feature: see recent note update
  As a learner, I want to see which of my notes are recently updated,
  so that I can focus on only reviewing the newly updated notes.

  Background:
    Given I've logged in as an existing user
    And I let the server to time travel to 100 hours ago
    And there are some notes for the current user
      | title    | testingParent |
      | NoteBook |               |
      | Note1    | NoteBook      |
      | Note2    | NoteBook      |
      | Note1.1  | Note1         |
      | Note1.2  | Note1         |
      | Note3    | NoteBook      |

  Scenario: I should see new note banner on newly updated note
    When I let the server to time travel to 24 hours ago
    And I update note "Note1.1" with the description "new description"
    Then I should see "Note1.1" is newer than "Note2"
    #And I should see "Note1" has recently updated descendant

@ignore
Feature: see recent note update
  As a learner, I want to see which of my notes are recently updated,
  so that I can focus on only reviewing the newly updated notes.

  Background:
    Given I've logged in as an existing user
    And there are some notes for the current user
      | title    | testingParent |
      | NoteBook |               |
      | Note1    | NoteBook      |
      | Note2    | NoteBook      |
      | Note1.1  | Note1         |
      | Note1.2  | Note1         |
      | Note3    | NoteBook      |
    And I update note "Note1.1" with the description "new description"

  Scenario: I should see new note banner on newly updated note
    Then I should see new note banner
    When I wait for 12 hours
    Then I should see 0 new note banner

  Scenario: I should see pink border around sub-notes that are newly updated
    When I open "NoteBook/Note1" note from top level
    Then I should see the sub-note "Note1.1" marked as new with a pink border
    When I wait for 12 hours
    Then I should see the sub-note "Note1.1" with the default border


  Scenario: I should see pink border around sub-notes that have newly updated sub-notes
    When I open "NoteBook" note from the top level
    Then I should see the sub-note "Note1" marked as new with a pink border
    When I wait for 12 hours
    Then I should see the sub-note "Note1" with the default border

Feature: undo note version
  As a learner, I want to be able to undo changes in an existing note. This will restore the previous version of the title and description on the existing note.

  @ignore
  Scenario: Undo changes in a note title
    Given an exiting note WITH TITLE "ABC"
    When a student update the note title to "XYZ"
    And student undo this change
    Then the the note Title should be "ABC"
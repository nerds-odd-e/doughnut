Feature: undo note version
  As a learner, I want to be able to undo changes in an existing note. This will restore the previous version of the title And description on the existing note.

  Background:
    Given I've logged in as an existing user

  @ignore
  Scenario: Undo changes in a note title
    Given there are some notes for the current user
      | title    |
      | ABC      |
    When I am editing note "ABC" the field should be pre-filled with
      | Title          |
      | ABC |
    When I update it to become:
      | Title          |
      | DEFG |
    And I click the undo button
    Then I should see these note
      | title     |
      | ABC |


  @ignore
  Scenario:
    Given an existing note WITH TITLE "ABC"
    When a student update the note title to "XYZ"
    And student undo this change
    And a connection issue happens
    Then the the note Title should be "XYZ"

  @ignore
  Scenario:
    Given an existing note WITH DESCRIPTION "DESCRIPTION 1"
    When a student update the note DESCRIPTION to "DESCRIPTION 2"
    And student undo this change
    Then the the note DESCRIPTION should be "DESCRIPTION 1"

  @ignore
  Scenario:
    Given an existing note WITH DESCRIPTION "DESCRIPTION 1"
    When a student update the note DESCRIPTION to "DESCRIPTION 2"
    And student undo this change
    And some connection issue happens
    Then the note DESCRIPTION should be "DESCRIPTION 2"

  @ignore
  Scenario:
    Given an existing note WITH TITLE "ABC"
    When a student update the note title to "XYZ"
    And student update it again to "IJK"
    And student undo the changes TWICE
    Then the note Title should be "ABC"
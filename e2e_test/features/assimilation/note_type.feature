Feature: Note Type
    As a learner, I want to assign a note type to my note

  Background:
    Given I am logged in as an existing user
    And there is a note "Unassigned Note"  

  Scenario: Allow user to assign note type during assimilation
    When I start assimilating "Unassigned Note"
    And I select note type "concept"
    Then the note "Unassigned Note" should be saved with note type "concept"
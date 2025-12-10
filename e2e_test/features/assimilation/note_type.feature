Feature: Note Type
    As a learner, I want to assign a note type to my note

  Background:
    Given I am logged in as an existing user

  Scenario: Allow user to assign note type during assimilation
      Given there is a note "Unassigned Note"
      When I start assimilating "Unassigned Note"
      Then I should be able to select a note type
      When I select note type "concept"
      Then the note "Unassigned Note" should be saved with note type "concept"
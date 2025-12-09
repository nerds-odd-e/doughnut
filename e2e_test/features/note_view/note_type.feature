@ignore
Feature: Note type
  As a learner, I want to add note type to my note

  Background:
    Given I am logged in as an existing user

  Scenario: Adding note type to my new note
    Given I have a notebook with the head note "Reservoirs" and details "to be changed"
    When I add note type "concept" to my note
    Then I will see new type "concept" on my note
    
Feature: split note
  As a learner, I want to split an existing note by each paragraph in each description
  where a paragraph is a group of lines, separated by empty lines.

  Background:
    Given I've logged in as an existing user

  Scenario: Each note has as title from the first line of a paragraph
    Given there are some notes for the current user
      | title   | description                                     |
      | animals | canine\nDogs, wolves, etc\n\nHomo\nWe are homos |
    When I split note "animals"
    Then I should see the note description to be ""
    And there is a note "animals/canine" with description "Dogs, wolves, etc"
    And there is a note "animals/Homo" with description "We are homos"

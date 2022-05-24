Feature: associate wiki to note
  As a user, I want to associate wikidata to a note.

  @ignore
  Scenario: Update wiki association for a note
    Given I've logged in as an existing user
    And there are some notes for the current user
      | title | description             |
      | TDD   | Test driven development |
    When I associate the note "TDD" with wikidata id "Q12345"
    Then I should see the wikidata url ""


Feature: associate wiki to note
  As a user, I want to associate wikidata to a note.

  Scenario: Associate wikidata ID for a note
    Given I've logged in as an existing user
    And there are some notes for the current user
      | title | description             |
      | TDD   | Test driven development |
    When I visit note "TDD"
    And I associate the note "TDD" with wikidata id "Q12345"
    Then I should see the icon beside title linking to wikidata url with id "Q12345"


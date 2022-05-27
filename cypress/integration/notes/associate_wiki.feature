Feature: associate wiki to note
  As a user, I want to associate wikidata to a note.

  Background:
    Given I've logged in as an existing user

  Scenario: Associate wikidata ID for a note
    Given there are some notes for the current user
      | title | description             |
      | TDD   | Test driven development |
    When I visit note "TDD"
    And I associate the note "TDD" with wikidata id "Q423392"
    Then I should see the icon beside title linking to wikidata url with id "Q423392"

  Scenario: Search wikidata link and associate one result for a note
    Given there are some notes for the current user
      | title | description            |
      | KFC   | Kentucky Fried Chicken |
    And there are some wikidata of KFC on the external service
    When I visit note "KFC"
    And I associate the note to wikidata by searching with "KFC"

  @ignore
  Scenario:
    Given there are some notes for the current user
      | title | description |
      | TDD   |             |
    And I visit note "TDD"
    And I associate the note "TDD" with wikidata id "Q12345"
    When I confirm the association with different title "Count von Count"
    Then I should see the icon beside title linking to wikipedia url


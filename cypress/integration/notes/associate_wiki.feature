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
    Then I should see the icon beside title linking to "wikipedia" url

    @ignore
  Scenario: Search wikidata link and associate one result for a note
    Given there are some notes for the current user
      | title | description            |
      | KFC   | Kentucky Fried Chicken |
    And there are some wikidata of KFC on the external service
    When I visit note "KFC"
    And I associate the note to wikidata by searching with "KFC"

  Scenario Outline: Associate note to wikipedia or wikidata if wikipedia does not exist
    Given there are some notes for the current user
      | title | description |
      | TDD   |             |
    And I visit note "TDD"
    And I associate the note "TDD" with wikidata id "<id>"
    When I confirm the association with different title "<title>"
    Then I should see the icon beside title linking to "<type>" url

    Examples:
      | id        | type      | title           |
      | Q12345    | wikipedia | Count von Count |
      | Q28799967 | wikidata  | Acanthias       |



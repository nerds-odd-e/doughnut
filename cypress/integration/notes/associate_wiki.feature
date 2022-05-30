Feature: associate wikidata ID to note

  Background:
    Given I've logged in as an existing user
    And I have a note with title "TDD"

  @ignore
  Scenario: Search wikidata link and associate one result for a note
    Given there are some wikidata of KFC on the external service
    When I visit note "TDD"
    And I associate the note to wikidata by searching with "TDD"

  Scenario Outline: Associate note to wikipedia or wikidata if wikipedia does not exist
    When I associate the note "TDD" with wikidata id "<id>"
    Then I <need to confirm> the association with different title "<title>"
    And I should see the icon beside title linking to "<type>" url

    Examples:
      | id        | need to confirm       | type      | title           |
      | Q423392   | don't need to confirm | wikipedia | TDD             |
      | Q12345    | need to confirm       | wikipedia | Count von Count |
      | Q28799967 | need to confirm       | wikidata  | Acanthias       |



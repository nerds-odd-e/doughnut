Feature: Validate Wikidata link

  Background:
    Given I've logged in as an existing user

  Scenario: Associate wikidata's id with note when wikidata's title is different from note's title
    Given there are some notes for the current user
      | title | description |
      | TDD   |             |
    And I visit note "TDD"
    And I associate the note "TDD" with wikidata id "Q12345"
    When I confirm the association with different title "Count von Count"
    Then I should see the icon beside title linking to wikidata url with id "Q12345"

  Scenario Outline: Cancel the association of the wikidata's id with note when wikidata's title is different from note's title
    Given there are some notes for the current user
      | title | description |
      | TDD   |             |
    And I visit note "TDD"
    And I associate the note "TDD" with wikidata id "<id>"
    When I cancel the association with different title "<title>"
    Then I should be able to return to the association dialog

    Examples:
      | id      | title           |
      | Q12345  | Count von Count |
      | Q123487 | Daniel Spoerri  |

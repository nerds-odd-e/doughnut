Feature: Validate Wikidata link

  Background:
    Given I've logged in as an existing user
    And I have a note with title "TDD"

  Scenario Outline: Cancel the association of the wikidata's id with note when wikidata's title is different from note's title
    Given I associate the note "TDD" with wikidata id "<id>"
    When I cancel the association with different title "<title>"
    Then I should be able to return to the association dialog

    Examples:
      | id      | title           |
      | Q12345  | Count von Count |
      | Q123487 | Daniel Spoerri  |

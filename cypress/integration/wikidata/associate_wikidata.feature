Feature: associate wikidata ID to note
  As a learner, I want to associate my note to wikidata IDs, so that I can
    * keep my concepts in sync with the rest of the world
    * get extensive content from Wikidata, Wikipedia and other knowledge base
    * Identify the duplicate note in my own notebooks and the circles I'm in

  Background:
    Given I've logged in as an existing user
    And I have a note with title "TDD"

  Scenario Outline: Associate note to wikipedia or wikidata
    When I associate the note "TDD" with wikidata id "<id>"
    Then I <need to confirm> the association with different title "<title>"
    And I should see the icon beside title linking to "<type>" url

    Examples:
      | id        | need to confirm       | type      | title           |
      | Q423392   | don't need to confirm | wikipedia | TDD             |
      | Q12345    | need to confirm       | wikipedia | Count von Count |
      | Q28799967 | need to confirm       | wikidata  | Acanthias       |

  @ignore
  Scenario: Search wikidata link and associate one result for a note
    Given there are some wikidata of KFC on the external service
    When I visit note "TDD"
    And I associate the note to wikidata by searching with "TDD"



Feature: Associate Wikidata ID to note
  As a learner, I want to associate my notes with Wikidata IDs, so that I can
  * keep my concepts in sync with the rest of the world
  * get extensive content from Wikidata, Wikipedia and other knowledge bases
  * identify duplicate notes in my notebooks and circles

  Background:
    Given I am logged in as an existing user
    And I have a notebook "TDD study" with a note "TDD"

  @usingMockedWikidataService
  Scenario: Association fails when Wikidata is unavailable
    Given The Wikidata service is not available
    And Wikidata search result always has "TDD" with ID "Q1"
    When I associate the note "TDD" with Wikidata ID "Q1"
    Then I should see the error "The wikidata service is not available" on Wikidata ID when associating

  @usingMockedWikidataService
  Scenario Outline: Associate note with Wikidata linking to Wikipedia when available
    Given Wikidata search result always has "TDD" with ID "<id>"
    And Wikidata.org has an entity "<id>" with label "TDD" and link to wikipedia "<wikipedia link>"
    When I associate the note "TDD" with Wikidata ID "<id>"
    Then the Wikidata association of note "TDD" should link to "<expected url>"

    Examples:
      | id | wikipedia link               | expected url                     |
      | Q1 |                              | https://www.wikidata.org/wiki/Q1 |
      | Q2 | https://en.wikipedia.org/TDD | https://en.wikipedia.org/TDD     |

  @usingRealWikidataService
  @skipOptimizationDueToKnownNecessarySlowness
  Scenario: Associate note via real Wikidata when labels differ
    When I associate the note "TDD" with Wikidata ID "Q12345"
    And I confirm the association using the suggested title "Count von Count"
    Then the Wikidata association on the current note should link to "https://en.wikipedia.org/wiki/Count_von_Count"

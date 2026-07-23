Feature: associate wikidata ID to note
    As a learner, I want to associate my note to wikidata IDs, so that I can
    * keep my concepts in sync with the rest of the world
    * get extensive content from Wikidata, Wikipedia and other knowledge base
    * Identify the duplicate note in my own notebooks and the circles I'm in

  Background:
    Given I am logged in as an existing user
    And I have a notebook "TDD study" with a note "TDD"

  @usingMockedWikidataService
  Scenario: Associate note to wikidata when the service is not available
    Given The wikidata service is not available
    And Wikidata search result always has "TDD" with ID "Q1"
    When I associate the note "TDD" with wikidata id "Q1"
    Then I should see an error "The wikidata service is not available" on Wikidata Id in association


  @usingMockedWikidataService
  Scenario Outline: Associate note to wikipedia via wikidata
    Given Wikidata search result always has "TDD" with ID "<id>"
    And Wikidata.org has an entity "<id>" with label "TDD" and link to wikipedia "<wikipedia link>"
    When I associate the note "TDD" with wikidata id "<id>"
    Then the Wiki association of note "TDD" should link to "<expected url>"

    Examples:
      | id | wikipedia link               | expected url                     |
      | Q1 |                              | https://www.wikidata.org/wiki/Q1 |
      | Q2 | https://en.wikipedia.org/TDD | https://en.wikipedia.org/TDD     |

  @usingRealWikidataService
  @skipOptimizationDueToKnownNecessarySlowness
  Scenario: Associate note to wikipedia via wikidata using real service
    When I associate the note "TDD" with wikidata id "Q12345"
    Then I need to confirm the association with different label "Count von Count"
    And the Wiki association on the current note should link to "https://en.wikipedia.org/wiki/Count_von_Count"

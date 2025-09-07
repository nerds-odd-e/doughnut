Feature: associate wikidata ID to note
    As a learner, I want to associate my note to wikidata IDs, so that I can
    * keep my concepts in sync with the rest of the world
    * get extensive content from Wikidata, Wikipedia and other knowledge base
    * Identify the duplicate note in my own notebooks and the circles I'm in

  Background:
    Given I am logged in as an existing user
    And I have a notebook with the head note "TDD"

  @usingMockedWikidataService
  Scenario: Associate note to wikidata when the service is not available
    Given The wikidata service is not available
    When I associate the note "TDD" with wikidata id "Q1"
    Then I should see an error "The wikidata service is not available" on "Wikidata Id"


  @usingMockedWikidataService
  Scenario Outline: Associate note to wikidata with validation
    Given Wikidata.org has an entity "<id>" with label "<wikidata label>"
    When I associate the note "TDD" with wikidata id "<id>"
    Then I <need to confirm> the association with different label "<wikidata label>"

    Examples:
      | id      | wikidata label  | need to confirm       |
      | Q423392 | TDD             | don't need to confirm |
      | Q12345  | Count von Count | need to confirm       |

  @usingMockedWikidataService
  Scenario Outline: Associate note to wikipedia via wikidata
    Given Wikidata.org has an entity "<id>" with label "TDD" and link to wikipedia "<wikipedia link>"
    When I associate the note "TDD" with wikidata id "<id>"
    Then the Wiki association of note "TDD" should link to "<expected url>"

    Examples:
      | id | wikipedia link               | expected url                     |
      | Q1 |                              | https://www.wikidata.org/wiki/Q1 |
      | Q2 | https://en.wikipedia.org/TDD | https://en.wikipedia.org/TDD     |

  @usingRealWikidataService
  Scenario: Associate note to wikipedia via wikidata using real service
    When I associate the note "TDD" with wikidata id "Q12345"
    Then I need to confirm the association with different label "Count von Count"
    And the Wiki association of note "TDD" should link to "https://en.wikipedia.org/wiki/Count_von_Count"


  @usingMockedWikidataService @mockBrowserTime
  Scenario: Associate a new note with exisitng wikidata id
    Given I have a notebook with head note "Animals" and notes:
      | Title            | Wikidata Id| Parent Title|
      | Dog              | Q123       | Animals     |
      | Canine           | Q456       | Animals     |
    And Wikidata.org has an entity "Q123" with label "Canine"
    And I change the note "Canine" to associate with wikidata id "Q123"
    Then I should see an error "Duplicate Wikidata ID Detected." on "Wikidata Id"

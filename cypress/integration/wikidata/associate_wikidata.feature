Feature: associate wikidata ID to note
    As a learner, I want to associate my note to wikidata IDs, so that I can
    * keep my concepts in sync with the rest of the world
    * get extensive content from Wikidata, Wikipedia and other knowledge base
    * Identify the duplicate note in my own notebooks and the circles I'm in

  Background:
    Given I've logged in as an existing user
    And I have a note with title "TDD"

  @usingDummyWikidataService
  Scenario: Associate note to wikidata when the service is not available
    Given The wikidata service is not available
    When I associate the note "TDD" with wikidata id "Q1"
    Then I should see a message "The wikidata service is not available"


  @usingDummyWikidataService
  Scenario Outline: Associate note to wikidata with validation
    Given Wikidata.org has an entity "<id>" with "<wikidata title>"
    When I associate the note "TDD" with wikidata id "<id>"
    Then I <need to confirm> the association with different title "<wikidata title>"

    Examples:
      | id      | wikidata title  | need to confirm       |
      | Q423392 | TDD             | don't need to confirm |
      | Q12345  | Count von Count | need to confirm       |

  @usingDummyWikidataService
  Scenario Outline: Associate note to wikipedia via wikidata
    Given Wikidata.org has an entity "<id>" with "TDD" and "<wikipedia link>"
    When I associate the note "TDD" with wikidata id "<id>"
    Then I should see the icon beside title linking to "<expected url>"

    Examples:
      | id | wikipedia link               | expected url                     |
      | Q1 |                              | https://www.wikidata.org/wiki/Q1 |
      | Q2 | https://en.wikipedia.org/TDD | https://en.wikipedia.org/TDD     |

  @usingRealWikidataService
  Scenario: Associate note to wikipedia via wikidata
    When I associate the note "TDD" with wikidata id "Q12345"
    Then I need to confirm the association with different title "Count von Count"
    And I should see the icon beside title linking to "https://en.wikipedia.org/wiki/Count_von_Count"

  @usingRealWikidataService
  Scenario: Updating existing note that has previously been associated with wikidata
    Given I have a note with title "TDD" associated with wikidata id "Q12345"
    When I associate the note "TDD" with a new wikidata id "Q6789"
    And When I revisit the associate wikidata dialog
    Then I should see that the placeholder containing the new wikidata id "Q6789"

  Scenario: Show wikidata id already associated with note
    Given I have a note with title "TDD" associated with wikidata id "Q12345"
    When I associate the note "TDD" with wikidata id "Q12345"
    Then I should see that the placeholder containing the new wikidata id "Q12345"

  Scenario: Updating existing note that has been associated with wikidata using the same wikidata id
    Given I have a note with title "TDD" associated with wikidata id "Q12345"
    When I associate the note "TDD" with wikidata id "Q12345"
    Then I should see a message "TDD is already associated with Q12345"

  @usingDummyWikidataService
  Scenario: Select one of the Wikidata entries from the search result
    Given I am creating note under "TDD"
    When I type "Rock" in the title and search on Wikidata
    And I select "rock music" with wikidataID "Q11399" from the Wikidata search result
    And I confirm that I want to replace the current title with the title from Wikidata
    Then I should see that the title is automatically populated with "rock music"
    Then I should see that the Wikidata ID is automatically populated with "Q11399"

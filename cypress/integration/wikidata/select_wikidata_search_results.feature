Feature: Select from Wikidata search results when creating child note
  I want to search the Wikidata for various content that could be related
  to my note title and when I select a Wikidata entry I want my current form
  to be automatically populated with the appropriate title and wikipedia

  Background:
    Given I've logged in as an existing user
    And I have a note with title "TDD"
    And I am creating note under "TDD"

  @usingRealWikidataService @ignore
  Scenario: Select one of the Wikidata entries from the search result
    When I type "Rock" in the title
    And I search on Wikidata for "Rock"
    And I select "rock music" with wikidataID "Q11399" from the Wikidata search result
    Then I should see that the title is automatically populated with "rock music"
    Then I should see that the Wikidata ID is automatically populated with "Q11399"


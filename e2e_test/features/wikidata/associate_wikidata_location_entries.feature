Feature: Create location notes from Wikidata
  As a learner, I want a location note from Wikidata to include geographic coordinates.

  Background:
    Given I am logged in as an existing user
    And I have a notebook "Places map"
    And Wikidata.org has an entity "Q334" with label "Singapore"
    And Wikidata.org entity "Q334" is a location at 1.3, 103.8

  @usingMockedWikidataService
  Scenario: Creating a location note from Wikidata fills coordinates
    When I create a note titled "Singapore" with Wikidata ID "Q334" in the notebook "Places map"
    Then the note content on the current page should be "Location: 1.3'N, 103.8'E"

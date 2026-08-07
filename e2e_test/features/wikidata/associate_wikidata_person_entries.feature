Feature: Create person notes from Wikidata
  As a learner, I want a person note from Wikidata to include birthday and country of origin.

  Background:
    Given I am logged in as an existing user
    And Wikidata.org has an entity "Q22502" with label "Taiwan"
    And Wikidata.org has an entity "Q736936" with label "Lu"
    And Wikidata.org has an entity "Q706446" with label "Wang Chien-ming"
    And Wikidata.org has an entity "Q4604" with label "Confucius"
    And Wikidata.org entity "Q706446" is a person from "Q22502" and birthday is "+1980-03-31T00:00:00Z"
    And Wikidata.org entity "Q4604" is a person from "Q736936" and birthday is "-0552-10-09T00:00:00Z"
    And I have a notebook "Notable people"

  @usingMockedWikidataService
  @skipOptimizationDueToKnownNecessarySlowness
  Scenario Outline: Creating a person note from Wikidata fills birthday and country
    When I create a note titled "<person name>" with Wikidata ID "<Wikidata ID>" in the notebook "Notable people"
    Then the note content on the current page should be "<expected content>"

    Examples:
      | person name     | Wikidata ID | expected content         |
      | Wang Chien-ming | Q706446     | Taiwan, 31 March 1980    |
      | Confucius       | Q4604       | Lu, 09 October 0553 B.C. |

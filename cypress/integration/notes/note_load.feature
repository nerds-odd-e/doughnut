Feature: note load

  Background:
    Given I've logged in as an existing user
    And there are some notes for the current user
      | title    | description    | testingParent |
      | AnyTitle | Any content    |               |
    
  @ignore
  Scenario: Create a location child note
    When I create a note belonging to "AnyTitle":
      | Title | Wikidata Id |
      | Seoul | Q8684       |
    Then I will see location photo and location map
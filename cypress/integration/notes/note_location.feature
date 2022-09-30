Feature: note location

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

  @ignore
  Scenario: Create a none location child note
    When I create a note belonging to "AnyTitle":
      | Title | Wikidata Id |
      | Human | Q706446     |
    Then I will not see location photo and location map

  @ignore
  @usingMockedWikidataService
  Scenario: Create a location child note but location map not exists
    Given location wikidataId "Q123456" and this wikidata only have location photo
    When I create a note belonging to "AnyTitle":
      | Title        | Wikidata Id |
      | fakeLocation | Q123456     |
    Then I will only see location photo

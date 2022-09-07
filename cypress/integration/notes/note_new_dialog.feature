Feature: New note creation prompt for title replacement

  Background:
    Given I've logged in as an existing user
    And there are some notes for the current user
      | title   | testingParent | description         |
      | Animals |               | An awesome training |

  @usingDummyWikidataService @mockBrowserTime
  Scenario: Create a new note with a wikidata id and replace title
    Given Wikidata has search result for "rocky" with wikidata ID "Q90"
    When I am creating a note under "My Notes/Animals"
    And I search with title "apple" on Wikidata
    And I select "rocky" with wikidataID "Q90" from the Wikidata search result
    And I check the checkbox to replace the title
    Then I should see that the "Title" becomes "rocky"

  @usingDummyWikidataService @mockBrowserTime
  Scenario Outline: Create a new note with a wikidata id without selecting replace title
    Given Wikidata has search result for "<dataSearchTitle>" with wikidata ID "Q90"
    When I am creating a note under "My Notes/Animals"
    And I search with title "<oldTitle>" on Wikidata
    And I select "<dataSearchTitle>" with wikidataID "Q90" from the Wikidata search result
    Then I should see that the "Title" becomes "<expectedTitle>"

    Examples:
      | dataSearchTitle | oldTitle | expectedTitle |
      | rocky           | apple    | apple         |
      | Apple           | apple    | Apple         |


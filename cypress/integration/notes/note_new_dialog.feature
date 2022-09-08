Feature: New note creation prompt for title replacement

  Background:
    Given I've logged in as an existing user
    And there are some notes for the current user
      | title   | testingParent | description         |
      | Animals |               | An awesome training |

  @usingDummyWikidataService @mockBrowserTime
  Scenario Outline: Ask when replacing title with wikidata suggested title when creating new note
    Given Wikidata has search result for "<dataSearchTitle>" with wikidata ID "Q90"
    When I am creating a note under "My Notes/Animals"
    And I search with title "<oldTitle>" on Wikidata
    And I select "<dataSearchTitle>" with wikidataID "Q90" from the Wikidata search result
    And I <action>
    Then I should see that the "Title" becomes "<expectedTitle>"

    Examples:
      | dataSearchTitle | action                 | oldTitle | expectedTitle |
      | rocky           | do not select anything | apple    | apple         |
      | Apple           | do not select anything | apple    | Apple         |
      | rocky           | select replace title   | apple    | rocky         |
      | rocky           | select append title    | apple    | apple / rocky |

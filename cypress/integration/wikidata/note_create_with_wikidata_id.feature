Feature: Nested Note Create with wikidata
  As a learner, I want to maintain my newly acquired knowledge in
  notes with an associated wikidata, so that I can review them in the future.

  Background:
    Given I've logged in as an existing user
    And there are some notes for the current user
      | title   | testingParent  | description         |
      | Animals |                | An awesome training |

  @usingDummyWikidataService @mockBrowserTime
  Scenario: Create a new note with a wikidata id
    Given Wikidata.org has an entity "Q2102" with "long animal"
    When I create a note belonging to "Animals":
      | Title |  Wikidata Id |
      | snake |  Q2102       |
    Then I should see the icon beside title linking to "https://www.wikidata.org/wiki/Q2102"

  @usingDummyWikidataService @mockBrowserTime
  Scenario: Create a new note with invalid wikidata id
    Given The wikidata service is not available
    When I try to create a note belonging to "Animals":
      | Title | Wikidata Id |
      | snake | Q12345R     |
    Then I should see a message "The wikidata service is not available"

  @usingDummyWikidataService @mockBrowserTime
  Scenario: Select one of the Wikidata entries from the search result
    Given Wikidata has search result for "rock music" with wikidata ID "Q11399"
    When I am creating a note under "My Notes/Animals"
    And I search with title "Rock" on Wikidata
    And I select "rock music" with wikidataID "Q11399" from the Wikidata search result
    Then I should see that the "Title" becomes "rock music"
    Then I should see that the "Wikidata Id" becomes "Q11399"

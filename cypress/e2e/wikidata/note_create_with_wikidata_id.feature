Feature: Nested Note Create with wikidata
  As a learner, I want to maintain my newly acquired knowledge in
  notes with an associated wikidata, so that I can review them in the future.

  Background:
    Given I've logged in as an existing user
    And there are some notes for the current user:
      | title   | testingParent | description         |
      | Animals |               | An awesome training |

  @usingMockedWikidataService @mockBrowserTime
  Scenario: Create a new note with a wikidata id
    Given Wikidata.org has an entity "Q2102" with title "long animal"
    When I create a note belonging to "Animals":
      | Title | Wikidata Id |
      | snake | Q2102       |
    Then the Wiki association of note "snake" should link to "https://www.wikidata.org/wiki/Q2102"

  @usingMockedWikidataService @mockBrowserTime
  Scenario: Create a new note with invalid wikidata id
    When I try to create a note belonging to "Animals":
      | Title | Wikidata Id |
      | snake | Q12345R     |
    Then I should see an error "The wikidata Id should be Q<numbers>" on "Wikidata Id"

  @usingMockedWikidataService @mockBrowserTime
  Scenario: Select one of the Wikidata entries from the search result
    Given Wikidata search result always has "Dog" with ID "Q11399"
    When I am creating a note under "My Notes/Animals"
    And I search with title "dog" on Wikidata
    And I select wikidataID "Q11399" from the Wikidata search result
    Then I should see that the "Title" becomes "Dog"
    Then I should see that the "Wikidata Id" becomes "Q11399"

  @usingMockedWikidataService @mockBrowserTime
  Scenario: Create a new note with duplicate wikidata id within the same notebook
    Given there are some notes for the current user:
      | title   | wikidataId | testingParent |
      | Star    |            |               |
      | Sun     | Q123       | Star          |
    When I try to create a note belonging to "Star":
      | Title   | Wikidata Id |
      | Solar   | Q123        |
    Then I should see an error "Duplicate Wikidata ID Detected." on "Wikidata Id"

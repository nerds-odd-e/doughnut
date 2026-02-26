Feature: Nested Note Create with wikidata
  As a learner, I want to maintain my newly acquired knowledge in
  notes with an associated wikidata, so that I can recall them in the future.

  Background:
    Given I am logged in as an existing user
    And I have a notebook with the head note "Animals"

  @usingMockedWikidataService @mockBrowserTime
  Scenario: Create a new note with a wikidata id
    Given Wikidata.org has an entity "Q2102" with label "long animal"
    When I create a note belonging to "Animals" with title "snake" and wikidata id "Q2102"
    Then the Wiki association of note "snake" should link to "https://www.wikidata.org/wiki/Q2102"

  @usingMockedWikidataService @mockBrowserTime
  Scenario: Create a new note with invalid wikidata id
    When I attempt to create a note belonging to "Animals" with title "snake" and wikidata id "Q12345R"
    Then I should see an error "The wikidata Id should be Q<numbers>" on Wikidata Id in note creation

  @usingMockedWikidataService @mockBrowserTime
  Scenario: Select one of the Wikidata entries from the search result
    Given Wikidata search result always has "Dog" with ID "Q11399"
    When I am creating a note under "Animals"
    And I search with phrase "dog" on Wikidata
    And I select wikidataID "Q11399" from the Wikidata search result
    Then I should see that the Title becomes "Dog"
    Then I should see that the Wikidata Id becomes "Q11399"

  @usingMockedWikidataService @mockBrowserTime
  Scenario: Create a new note with duplicate wikidata id within the same notebook
    Given I have a notebook with head note "Star" and notes:
      | Title            | Wikidata Id| Parent Title|
      | Sun              | Q123       | Star        |
    When I attempt to create a note belonging to "Star" with title "Solar" and wikidata id "Q123"
    Then I should see an error "Duplicate Wikidata ID Detected." on Wikidata Id in note creation

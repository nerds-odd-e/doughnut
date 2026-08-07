Feature: Create notes with Wikidata ID
  As a learner, I want to create notes associated with Wikidata, so that I can recall them later.

  Background:
    Given I am logged in as an existing user
    And I have a notebook "Wildlife notes"

  @usingMockedWikidataService @mockBrowserTime
  Scenario: Create a note with a Wikidata ID
    Given Wikidata.org has an entity "Q2102" with label "long animal"
    When I create a note titled "snake" with Wikidata ID "Q2102" in the notebook "Wildlife notes"
    Then the Wikidata association on the current note should link to "https://www.wikidata.org/wiki/Q2102"

  @usingMockedWikidataService @mockBrowserTime
  Scenario: Reject invalid Wikidata ID when creating a note
    When I attempt to create a note titled "snake" with Wikidata ID "Q12345R" in the notebook "Wildlife notes"
    Then I should see the error "The wikidata Id should be Q<numbers>" on Wikidata ID when creating a note

  @usingMockedWikidataService @mockBrowserTime
  Scenario: Choose a Wikidata search result while creating a note
    Given Wikidata search result always has "Dog" with ID "Q11399"
    When I am creating a note in the notebook "Wildlife notes"
    And I search Wikidata for "dog"
    And I select Wikidata ID "Q11399" from the search results
    Then the note creation Title should be "Dog"
    And the note creation Wikidata ID should be "Q11399"

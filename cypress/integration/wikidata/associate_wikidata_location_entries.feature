Feature: Note creation/edit should have description if wikidata is a location
  As a learner, I want to to create a note. If the note is a location I want the location
  Longitude and latitude to be included in the description of the new note.

  Background:
    Given I've logged in as an existing user
    And there are some notes for the current user
      | title  | description |
      | places | some desc   |
    And Wikidata.org has an entity "Q334" with title "Singapore"
    And Wikidata.org entity "Q334" is a location at 1.3, 103.8

  @usingMockedWikidataService
  Scenario: New Note creation and wikidata is selected by user
    When I create a note belonging to "places":
      | Title     | Wikidata Id  |
      | Singapore | Q334         |
    Then I should see the description becomes "Location: 1.3'N, 103.8'E"

  @usingMockedWikidataService
  Scenario: Existing Note wikidata edited by user
    And there are some notes for the current user
      | title      | description  | testingParent |
      | Singapore  | The red dot  | places        |
    And I associate the note "Singapore" with wikidata id "Q334"
    And I should see the description becomes "Location: 1.3'N, 103.8'E The red dot"

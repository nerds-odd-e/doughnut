Feature: Note creation/edit for a location
  As a learner
  I want to to create a note for a location
  So that a note can be created with a details, location(longitude and latitude), map and image

  Background:
    Given I am logged in as an existing user
    And I have a notebook with the head note "places"
    And Wikidata.org has an entity "Q334" with label "Singapore"
    And Wikidata.org entity "Q334" is a location at 1.3, 103.8

  @usingMockedWikidataService
  Scenario: New Note creation and wikidata is selected by user
    When I create a note belonging to "places" with title "Singapore" and wikidata id "Q334"
    Then the note details on the current page should be "Location: 1.3'N, 103.8'E"

  @usingMockedWikidataService
  Scenario: Existing Note wikidata edited by user
    When there are some notes:
      | Title            | Details     | Parent Title|
      | Singapore        | The red dot | places      |
    And I associate the note "Singapore" with wikidata id "Q334"
    Then the note details on the current page should be "Location: 1.3'N, 103.8'E\nThe red dot"

  @usingMockedWikidataService
  Scenario: A note can be created for a location with a map and identifying image
    When I create a note belonging to "places" with title "Singapore" and wikidata id "Q334"
    Then a map pointing to lat: "1.3", lon: "103.8" is added to the note

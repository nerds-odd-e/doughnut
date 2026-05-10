Feature: Note creation for a location
  As a learner
  I want to create a note for a location
  So that a note can be created with content, location (longitude and latitude), map and image

  Background:
    Given I am logged in as an existing user
    And I have a notebook "Places map"
    And Wikidata.org has an entity "Q334" with label "Singapore"
    And Wikidata.org entity "Q334" is a location at 1.3, 103.8

  @usingMockedWikidataService
  Scenario: New Note creation and wikidata is selected by user
    When I create a note with title "Singapore" and wikidata id "Q334" in the notebook "Places map"
    Then the note content on the current page should be "Location: 1.3'N, 103.8'E"

  @usingMockedWikidataService
  Scenario: A note can be created for a location with a map and identifying image
    When I create a note with title "Singapore" and wikidata id "Q334" in the notebook "Places map"
    Then a map pointing to lat: "1.3", lon: "103.8" is added to the note

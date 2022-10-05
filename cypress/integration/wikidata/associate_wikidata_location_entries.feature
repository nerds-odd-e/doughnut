Feature: Note creation/edit for a location
  As a learner
  I want to to create a note for a location
  So that a note can be created with a description, location(longitude and latitude), map and picture

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
    Then I should see the note description on current page becomes "Location: 1.3'N, 103.8'E"
#    And I should see the "Map" icon beside title "Singapore" linking to "https://geohack.toolforge.org/geohack.php?params=1.3_N_103.8_E_globe:earth&language=en"

  @usingMockedWikidataService
  Scenario: Existing Note wikidata edited by user
    And there are some notes for the current user
      | title      | description  | testingParent |
      | Singapore  | The red dot  | places        |
    And I associate the note "Singapore" with wikidata id "Q334"
    And I should see the note description on current page becomes "Location: 1.3'N, 103.8'E The red dot"

  @usingMockedWikidataService
  @ignore
  Scenario: A note can be created for a location with a map and identifying picture
    When I create a note belonging to "places":
      | Title     | Wikidata Id  |
      | Singapore | Q334         |
    Then a map is added to the note
    And an identifying picture is shown

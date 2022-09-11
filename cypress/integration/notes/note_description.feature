Feature: Note creation/edit should have description if wikidata is a location
  As a learner, I want to to create a note. If the note is a location I want the location
  Longitude and latitude to be included in the description of the new note.

  Background:
    Given I've logged in as an existing user
    And there are some notes for the current user
      | title  | description |
      | places | some desc   |

  @usingMockedWikidataService
  Scenario: New Note creation and wikidata is selected by user
    Given Wikidata.org has an entity "Q334" with title "Singapore"
    And Wikidata.org entity "Q334" is a location at 1.3, 103.8
    When I create a note belonging to "places":
      | Title     | Wikidata Id  |
      | Singapore | Q334         |
    Then I should see the description becomes "Location: 1.3'N, 103.8'E"

  @usingRealWikidataService
  Scenario Outline: Existing Note wikidata edited by user
    And there are some notes for the current user
      | title      | description    | testingParent |
      | <OldTitle> | <InitialText>  | places        |
    When I navigate to "My Notes/places/Singapore" note
    And I associate the current note with wikidata id "<NewWikiId>"
    And  I need to confirm the association with different title "<NewTitle>"
    Then I should see the icon beside title linking to "https://en.wikipedia.org/wiki/<NewTitle>"
    And I should see the description becomes "<FinalText>"

    Examples:
      | OldTitle  | WikidataId | NewTitle | NewWikiId | InitialText              | FinalText                                     |
      | Singapore | Q334       | Germany  | Q183      | Location: 1.3'N, 103.8'E | Location: 51'N, 10'E Location: 1.3'N, 103.8'E |

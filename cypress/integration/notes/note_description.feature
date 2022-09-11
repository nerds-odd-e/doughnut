Feature: Note creation/edit should have description if wikidata is a location
  As a learner, I want to to create a note. If the note is a location I want the location
  Longitude and latitude to be included in the description of the new note.

  Background:
    Given I've logged in as an existing user
    And there are some notes for the current user
      | title  | description |
      | places | some desc   |

  @usingMockedWikidataService
  Scenario Outline: New Note creation and wikidata is selected by user
    Given Wikidata.org has an entity "<WikidataId>" with "<Title>"
    And Wikidata.org entity "<WikidataId>" is a location at <Lat>, <Lng>
    When I create a note belonging to "places":
      | Title   | Wikidata Id     |
      | <Title> | <WikidataId>    |
    Then I should see the description becomes "<Text>"

    Examples:
      | Title     | Lat | Lng   | Text                     | WikidataId |
      | Singapore | 1.3 | 103.8 | Location: 1.3'N, 103.8'E | Q334       |
      | Germany   | 51  | 10    | Location: 51'N, 10'E     | Q183       |

  @usingRealWikidataService
  Scenario Outline: Existing Note wikidata edited by user
    Given I am creating a note under "My Notes/places"
    When I create a note belonging to "places":
      | Title      | Wikidata Id     |
      | <OldTitle> | <WikidataId>    |
    Then I should see the description becomes "<InitialText>"
    When I navigate to "My Notes/places/Singapore" note
    And I associate the current note with wikidata id "<NewWikiId>"
    And  I need to confirm the association with different title "<NewTitle>"
    Then I should see the icon beside title linking to "https://en.wikipedia.org/wiki/<NewTitle>"
    And I should see the description becomes "<FinalText>"

    Examples:
      | OldTitle  | WikidataId | NewTitle | NewWikiId | InitialText              | FinalText                                     |
      | Singapore | Q334       | Germany  | Q183      | Location: 1.3'N, 103.8'E | Location: 51'N, 10'E Location: 1.3'N, 103.8'E |

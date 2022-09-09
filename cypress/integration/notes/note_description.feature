Feature: Note creation/edit should have description if wikidata is a location
  As a learner, I want to to create a note. If the note is a location I want the location
  Longitude and latitude to be included in the description of the new note.

  Background:
    Given I've logged in as an existing user
    And there are some notes for the current user
      | title  | description |
      | places | some desc   |

  @usingRealWikidataService
  Scenario Outline: New Note creation and wikidata is selected by user
    Given I am creating a note under "My Notes/places"
    When I create a note belonging to "places":
      | Title   | Wikidata Id |
      | <Title> | <WikiID>    |
    Then I should see the description with "<Text>"

    Examples:
      | Title     | Text                     | WikiID    |
      | Singapore | Location: 1.3'N, 103.8'E | Q334      |
      | Germany   | Location: 51'N, 10'E     | Q183      |

  @ignore @usingRealWikidataService
  Scenario Outline: Existing Note wikidata edited by user
    Given I am creating a note under "My Notes/places"
    When I create a note belonging to "places":
      | Title      | Wikidata Id |
      | <OldTitle> | <WikiId>    |
    Then I should see the description with "<InitialText>"
    When I navigate to "My Notes/places/Singapore" note
    And I associate the current note with wikidata id "<NewWikiId>"
    And  I need to confirm the association with different title "<NewTitle>"
    Then I should see the icon beside title linking to "https://en.wikipedia.org/wiki/<NewTitle>"
    And I should see the description with "<FinalText>"

    Examples:
      | OldTitle  | WikiId    | NewTitle | NewWikiId | InitialText              | FinalText                                       |
      | Singapore | Q334      | Germany  | Q183      | Location: 1.3'N, 103.8'E | Location: 51'N, 10'E \nLocation: 1.3'N, 103.8'E |
      | Singapore | Q334      | Jackie   | Q16277237 | Location: 1.3'N, 103.8'E | Location: 1.3'N, 103.8'E                        |
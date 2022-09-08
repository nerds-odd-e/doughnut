Feature: Note creation/edit should have description if wikidata is a location
  As a learner, I want to to create a note. If the note is a location I want the location
  Longitude and latitude to be included in the description of the new note.

  Background:
    Given I've logged in as an existing user
    And there are some notes for the current user
      | title  | description |
      | places | some desc   |

  @ignore @useRealService
  Scenario Outline: New Note creation and wikidata is selected by user
    Given I am creating a note under "My Notes/places"
    When I create a note belonging to "places":
      | Title   | Wikidata Id |
      | <Title> | Q334        |
    Then I should see the description with "<descriptionText>"

    Examples:
      | Title     | descriptionText          |
      | Singapore | Location: 1.3'N, 103.8'E |
      | Germany   | Location: 51'N, 10'E     |
      | Covid     |                          |

  @ignore @useRealService
  Scenario Outline: Existing Note wikidata edited by user
    Given I am creating a note under "My Notes/places"
    When I create a note belonging to "places":
      | Title   | Wikidata Id |
      | <Title> | Q334        |
    Then I should see description with "<initialText>"
    When I edit the WID of the note to "Q183"
    Then I should see the description with "<finalText>"

    Examples:
      | Title     | initialText              | finalText                                     |
      | Singapore | Location: 1.3'N, 103.8'E | Location: 51'N, 10'E Location: 1.3'N, 103.8'E |

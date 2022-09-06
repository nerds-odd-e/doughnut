Feature: New note creation should have description if wikidata is a location
  As a learner, I want to to create a note. If the note is a location I want the location 
  Longitude and latitude to be included in the description of the new note.

  Background:
    Given I've logged in as an existing user
    And when I want to create a new note where the note title is a location
  @ignore
  Scenario Outline: New Note creation and wikidata is selected by user
    Given I click submit on new note creation
    Then I select the item from the search result list and click submit
    Then I should see <LATLNG> getting prepend into note description
    
    Examples:
      | title      | Location           |
      | Singapore  | 1°18'N, 103°48'E   |
      | Bangkok    | 2°18'N, 89°48'E    |
      | Covid      |                    |


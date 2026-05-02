Feature: Note creation for a book

  Background:
    Given I am logged in as an existing user
    And I have a notebook "Favorite reads" with a note "My Favourite Books" and notes:
      | Title | Folder             |
      | keep  | My Favourite Books |

  @usingMockedWikidataService
  Scenario: Create a new book note with authors as sibling notes
    Given Wikidata.org has an entity "Q45575" with label "Dennis Ritchie"
    And Wikidata.org has an entity "Q92608" with label "Brian Kernighan"
    And Wikidata.org has an entity "Q1137974" with label "The C Programming Language"
    And the Wikidata.org entity "Q1137974" is written by authors with ID
      | Wikidata Id     |
      | Q45575          |
      | Q92608          |
    When I create a note belonging to "My Favourite Books" with title "The C Programming Language" and wikidata id "Q1137974"
    Then I should see folder "Favorite reads/My Favourite Books" containing these notes:
      | note-title                 |
      | keep                       |
      | The C Programming Language |
      | Dennis Ritchie             |
      | Brian Kernighan            |

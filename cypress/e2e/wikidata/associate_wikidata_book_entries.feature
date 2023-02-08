Feature: Note creation for a book

  Background:
    Given I've logged in as an existing user
    And there are some notes for the current user
      | title              |
      | My Favourite Books |

  @usingMockedWikidataService
  Scenario: Create a new book note with author note not exists in notebook
    Given Wikidata.org has an entity "Q34668" with title "J. K. Rowling"
    And Wikidata.org has an entity "Q8337" with title "Harry Potter"
    And the Wikidata.org entity "Q8337" is written by an author with ID "Q34668"
    When I create a note belonging to "My Favourite Books":
      | Title         | Wikidata Id  |
      | Harry Potter  | Q8337        |
    Then I should see "My Notes/My Favourite Books/Harry Potter" with these children
      | note-title    |
      | J. K. Rowling |

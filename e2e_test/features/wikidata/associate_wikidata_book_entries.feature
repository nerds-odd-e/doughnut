Feature: Note creation for a book

  Background:
    Given I am logged in as an existing user
    And I have a note with the topic "My Favourite Books"

  @usingMockedWikidataService
  Scenario: Create a new book note with multiple authors as children notes
    Given Wikidata.org has an entity "Q45575" with label "Dennis Ritchie"
    And Wikidata.org has an entity "Q92608" with label "Brian Kernighan"
    And Wikidata.org has an entity "Q1137974" with label "The C Programming Language"
    And the Wikidata.org entity "Q1137974" is written by authors with ID
      | Wikidata Id     |
      | Q45575          |
      | Q92608          |
    When I create a note belonging to "My Favourite Books":
      | Topic                         | Wikidata Id     |
      | The C Programming Language    | Q1137974        |
    Then I should see "My Notes/My Favourite Books/The C Programming Language" with these children
      | note-topic      |
      | Dennis Ritchie  |
      | Brian Kernighan |

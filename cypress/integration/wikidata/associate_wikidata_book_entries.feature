Feature: Note creation should have a child/children note with author name if wikidata is a book
  As a learner, I want to to create a note. If the note is a book I want create the new child note for each authors

  Background:
    Given I've logged in as an existing user
    And there are some notes for the current user
      | title  | description |
      | books  | some desc   |
    And Wikidata.org has an entity "Q5140024" with title "code complete"
    And Wikidata.org entity "Q5140024" is a book with authors
      | Title         |
      | Steve McConnell|
  @ignore
  @usingMockedWikidataService
  Scenario: New Note creation and wikidata is selected by user and there is only one the book author.
    When I create a note belonging to "books":
      | Title           | Wikidata Id |
      | code complete   | Q5140024    |
    Then I should see "code complete" with these children
      | note-title     |
      | Steve McConnell|


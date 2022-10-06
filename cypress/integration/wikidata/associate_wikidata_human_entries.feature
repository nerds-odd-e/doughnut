Feature: Note creation should have description if wikidata is a human
  As a learner, I want to to create a note. If the note is a human I want the birthday and country
  to be included in the description of the new note.

  Background:
    Given I've logged in as an existing user
    And there are some notes for the current user
      | title  | description |
      | places | some desc   |
    And Wikidata.org has an entity "Q334" with title "Singapore"

  @usingMockedWikidataService
  @ignore
  Scenario Outline: New Note creation and human wikidata is selected
    When I create a note belonging to "<name>" with id "<wikidataId>"
    Then I should see the note description on current page becomes "<description>"
    Examples:
      | name            | wikidataId | description |
      | Wang Chien-ming | Q706446    | Taiwan, 31 March 1980 |
      | Confucius       | Q4604      | Lu, 09 October 0552 B.C. |

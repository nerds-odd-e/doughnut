Feature: Note creation should have description if wikidata is a person
  As a learner, I want to to create a note. If the note is a person I want the birthday and country
  to be included in the description of the new note.

  Background:
    Given I've logged in as an existing user
    And I create a notebook with title

  Scenario Outline: New Note creation and person wikidata is selected
    When I create a note with title "<name>" and wiki id "<wikidataId>"
    When I should see the note description on current page becomes "<description>"
    Examples:
      | name            | wikidataId | description |
      | Wang Chien-ming | Q706446    | Taiwan, 31 March 1980 |
      | Confucius       | Q4604      | Lu, 09 October 0552 B.C. |

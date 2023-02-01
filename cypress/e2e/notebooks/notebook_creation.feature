Feature: Notebook creation

  Background:
    Given I've logged in as an existing user

  Scenario: Create two new notebooks
    When I create notebooks with:
      | Title    | Description     | Upload Picture    | Picture Url | Picture Mask |
      | Sedation | Put to sleep    | example-large.png |             | 20 40 70 30  |
      | Sedition | Incite violence |                   | a_slide.jpg |              |
    Then I should see these notes belonging to the user at the top level of all my notes
      | title    |
      | Sedation |
      | Sedition |
    And I navigate to "My Notes/Sedation" note
    And I should see the screenshot matches

  Scenario: Create a new note with invalid information
    When I create a notebook with empty title
    Then I should see that the note creation is not successful

  Scenario Outline: Create a new book note with author note not exists in notebook
    Given Wikidata.org has an entity "<wikidataId>" with title "<book name>"
    And the Wikidata.org entity "<wikidataId>" is written by "<author name>" with "<author wikidataId>"
    And there are some notes for the current user
      | title            |
      | <note book name> |
    When I create a note belonging to "<note book name>":
      | Title         | Wikidata Id  |
      | <book name>   | <wikidataId> |
#    Then new note with title "<book name>" is created with child Note with name "<author name>" in noteBook "<note book name>"

    Examples:
      | wikidataId | book name    | author wikidataId | author name   | note book name     |
      | Q8337      | Harry Potter | Q34660            | J. K. Rowling | My Favourite Books |

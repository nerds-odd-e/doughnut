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

  Scenario: Create a new book note with author note not exists in Notebook
    Given in Wikidata there is a book "LOTRWikidataId" with author "LOTRAuthorWikidataId" and author name is "J K Rowling"
    And there is no note with wikidataId "LOTRAuthorWikidataId" in noteBook "MyNoteBook"
    When I create the new note titled "LOTR" with wikidataId "LOTRWikidataId" in noteBook "MyNoteBook"
    Then new note with title "LOTR" is created with child Note with name "J K Rowling" in noteBook "MyNoteBook"

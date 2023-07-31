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

@ignore
  Scenario: Create a new note with invalid information
    When I create a notebook with empty title
    Then I should see that the note creation is not successful

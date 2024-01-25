Feature: Notebook creation

  Background:
    Given I am logged in as an existing user

  Scenario: Create two new notebooks
    When I create notebooks with:
      | Topic    | Details         | Upload Picture    | Picture Url | Picture Mask |
      | Sedation | Put to sleep    | example-large.png |             | 20 40 70 30  |
      | Sedition | Incite violence |                   | a_slide.jpg |              |
    Then I should see these notes belonging to the user at the top level of all my notes
      | topic    |
      | Sedation |
      | Sedition |
    And I navigate to "My Notes/Sedation" note
    And I should see the screenshot matches

  Scenario: Create a new note with invalid information
    When I create a notebook with empty topic
    Then I should see that the note creation is not successful

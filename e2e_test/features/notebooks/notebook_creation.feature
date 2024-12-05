Feature: Notebook creation

  Background:
    Given I am logged in as an existing user

  Scenario: Create a new notebooks
    When I create a notebook with topic "Sedation"
    Then I should see these notes belonging to the user at the top level of all my notes
      | Title    |
      | Sedation |

  Scenario: Create a new note with invalid information
    When I create a notebook with empty topic
    Then I should see that the note creation is not successful

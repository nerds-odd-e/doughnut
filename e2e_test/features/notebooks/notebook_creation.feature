Feature: Notebook creation

  Background:
    Given I am logged in as an existing user

  Scenario: Create notebook with description
    When I create a notebook with title "Sedation Wiki" and description "Quick reference for sedation protocols"
    Then I should see these notes belonging to the user at the top level of all my notes
      | Title          | Description                                |
      | Sedation Wiki  | Quick reference for sedation protocols     |

  Scenario: Create a new notebook with invalid information
    When I create a notebook with empty title
    Then I should see that the note creation is not successful

Feature: Notebook creation

  Background:
    Given I am logged in as an existing user

  Scenario: Create notebook with description
    When I create a notebook with title "Sedation Wiki" and description "Quick reference for sedation protocols"
    Then I should see my notebooks:
      | Title          | Description                                |
      | Sedation Wiki  | Quick reference for sedation protocols     |

  Scenario: Add first top-level note from notebook page when notebook has no index
    When I create a notebook with title "Empty NB E2E Root" and description "e2e"
    And I open the notebook "Empty NB E2E Root" from my notebooks catalog
    And I add the first note from the empty notebook page
    Then the note title should be "Untitled"

  Scenario: Create a new notebook with invalid information
    When I create a notebook with empty title
    Then I should see that the note creation is not successful

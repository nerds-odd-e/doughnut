Feature: Notebook creation

  Background:
    Given I am logged in as an existing user

  Scenario: Create notebook with description
    When I create a notebook with title "Sedation Wiki" and description "Quick reference for sedation protocols"
    Then I should see my notebooks:
      | Title          | Description                                |
      | Sedation Wiki  | Quick reference for sedation protocols     |

  Scenario: Create notebook index from notebook page when notebook has no index
    When I create a notebook with title "Empty NB E2E Index" and description "e2e"
    And I open the notebook "Empty NB E2E Index" from my notebooks catalog
    And I type notebook index body "E2E index body" on the notebook page and save
    And I reload the notebook page
    Then the notebook index body includes "E2E index body"

  Scenario: Create a new notebook with invalid information
    When I create a notebook with empty title
    Then I should see that the note creation is not successful

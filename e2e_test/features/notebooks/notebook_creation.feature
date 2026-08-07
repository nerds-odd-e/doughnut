Feature: Notebook creation

  Background:
    Given I am logged in as an existing user

  Scenario: Create notebook with description
    When I create a notebook with title "Sedation Wiki" and description "Quick reference for sedation protocols"
    Then I should see my notebooks:
      | Title          | Description                                |
      | Sedation Wiki  | Quick reference for sedation protocols     |

  Scenario: Create notebook readme from notebook page when notebook has no readme
    Given I have a notebook "Empty NB E2E Readme"
    And I open the notebook "Empty NB E2E Readme" from my notebooks catalog
    When I save notebook readme "E2E readme body"
    And I reload the notebook page
    Then the notebook readme body includes "E2E readme body"

  Scenario: Create a new notebook with invalid information
    When I create a notebook with empty title
    Then I should see that the notebook creation is not successful

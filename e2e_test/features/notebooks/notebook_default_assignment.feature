Feature: Notebook default assignment

  Background:
    Given I am logged in as an existing user
      And there are some notes:
      | Title        | Details         | Parent Title |
      | Notebook One | First notebook  | Notebook One |
      | Notebook Two | Second notebook | Notebook Two |

  @ignore
  Scenario: Assign notebook as default
    When I select the notebook with title "Notebook Two" as default
    Then the notebook "Notebook Two" is the default for existing user

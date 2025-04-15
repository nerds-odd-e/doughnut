Feature: Notebook default assignment

  Background:
    Given I am logged in as existing user
      And there are some notes
      | Title        | Details         | Parent Title |
      | Notebook One | First notebook  | Notebook One |
      | Notebook Two | Second notebook | Notebook Two |

  @ignore
  Scenario: Assign notebook as default
    When existing user selects Notebook Two as default
    Then Notebook Two is the default for existing user

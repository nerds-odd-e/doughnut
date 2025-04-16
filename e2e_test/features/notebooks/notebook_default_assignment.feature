Feature: Notebook default assignment

  Background:
    Given I am logged in as an existing user
      And I create a notebook with the title "Notebook One"
      And I create a notebook with the title "Notebook Two"

  @ignore
  Scenario: Assign notebook as default
    When I select the notebook with title "Notebook Two" as default
    Then the notebook "Notebook Two" is the default for existing user

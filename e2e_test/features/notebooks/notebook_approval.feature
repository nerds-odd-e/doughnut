@skip
Feature: Notebook approval

  Background:
    Given I am logged in as an existing user
    And I have a notebook with the head note "Sedation"

  Scenario: Apply for an approval for a notebook
    When I apply for an approval for notebook "Sedation"
    Then I should see the status "pending" of the approval for notebook "Sedation"

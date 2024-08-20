  Feature: Notebook approval

  Background:
    Given I am logged in as an existing user
    And I have a notebook with head note "LeSS in Action"

  Scenario: Apply for an approval for a notebook
    When I open a settings menu in my notebook "LeSS in Action" page
    Then I should see an request for approval button
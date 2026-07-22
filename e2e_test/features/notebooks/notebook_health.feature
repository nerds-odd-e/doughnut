Feature: Notebook health
  As a notebook owner, I want to run lint from the Health tab
  so that I can review empty folders, readme-only folders, and dead wiki links without mutating data.

  Background:
    Given I am logged in as an existing user

  Scenario: Run lint shows expandable findings for seeded health issues
    Given I have a notebook "Health findings suite" with a note "Carrier" and content "See [[Missing]]"
    When I view note "Carrier"
    And I create a folder named "Empty Shell" while viewing note "Carrier"
    And I create a folder named "Readme Only Shell" while viewing note "Carrier"
    And I open the folder page for "Readme Only Shell" from the sidebar
    And I type and save the folder readme with text "Only a readme here"
    And I open the notebook "Health findings suite" from my notebooks catalog
    And I open the notebook workspace Health tab
    Then the notebook health idle prompt is visible
    When I run notebook health lint
    Then the notebook health findings show expandable groups for empty folders, readme-only folders, and dead wiki links
    And the notebook health empty folders finding includes "Empty Shell"
    And the notebook health readme-only folders finding includes "Readme Only Shell"
    And the notebook health dead wiki links finding includes note "Carrier" and token "Missing"

  Scenario: Run lint with Remove empty folders checked does not delete folders
    Given I have a notebook "Health no-mutate suite" with a note "Anchor"
    When I view note "Anchor"
    And I create a folder named "Keep Empty" while viewing note "Anchor"
    And I open the notebook "Health no-mutate suite" from my notebooks catalog
    And I open the notebook workspace Health tab
    And I check Remove empty folders on the notebook health panel
    And I run notebook health lint
    Then I should see sidebar folder "Keep Empty"

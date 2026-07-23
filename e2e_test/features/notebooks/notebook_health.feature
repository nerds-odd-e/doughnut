Feature: Notebook health
  As a notebook owner, I want to run lint from the Health tab
  so that I can review empty folders, readme-only folders, and dead wiki links without mutating data.

  Background:
    Given I am logged in as an existing user

  Scenario: Run lint shows expandable findings for seeded health issues
    Given I have a notebook "Health findings suite" with a note "Carrier" and content "See [[Missing]]"
    And the notebook "Health findings suite" has an empty folder "Empty Shell"
    And the notebook "Health findings suite" has a readme-only folder "Readme Only Shell" with readme "Only a readme here"
    When I jump to the notebook "Health findings suite"
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

  Scenario: Gated fix removes fully empty folders and keeps readme-only
    Given I have a notebook "Health purge suite" with a note "Anchor"
    When I view note "Anchor"
    And I create a folder named "Empty Shell" while viewing note "Anchor"
    And I create a folder named "Readme Only Shell" while viewing note "Anchor"
    And I open the folder page for "Readme Only Shell" from the sidebar
    And I type and save the folder readme with text "Only a readme here"
    And I open the notebook "Health purge suite" from my notebooks catalog
    And I open the notebook workspace Health tab
    And I check Remove empty folders on the notebook health panel
    And I run notebook health lint
    And the notebook health empty folders finding includes "Empty Shell"
    And the notebook health readme-only folders finding includes "Readme Only Shell"
    When I apply notebook health empty folder fix
    Then the notebook health empty folders finding does not include "Empty Shell"
    And the notebook health readme-only folders finding includes "Readme Only Shell"
    And I should not see sidebar folder "Empty Shell"
    And I should see sidebar folder "Readme Only Shell"

  Scenario: Save Remove empty folders default applies on another notebook
    Given I have a notebook "Defaults A" with a note "A1"
    And I have a notebook "Defaults B" with a note "B1"
    When I open the notebook "Defaults A" from my notebooks catalog
    And I open the notebook workspace Health tab
    And I check Remove empty folders on the notebook health panel
    And I save notebook health options as defaults
    And I open the notebook "Defaults B" from my notebooks catalog
    And I open the notebook workspace Health tab
    Then Remove empty folders on the notebook health panel is checked

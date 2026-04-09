Feature: Notebook group

  Background:
    Given I am logged in as an existing user
    And I have a notebook with head note "Group E2E Root" and notes:
      | Title | Parent Title   |
      | child | Group E2E Root |

  Scenario: Create a group, assign a notebook, see a member hint, then ungroup
    When I create a notebook group named "My E2E Group" from my notebooks page
    And I assign notebook "Group E2E Root" to notebook group "My E2E Group"
    When I go to my notebooks page
    Then I should see notebook group "My E2E Group" with a hint including "Group E2E Root"
    When I set notebook "Group E2E Root" to ungrouped on the notebook settings page
    When I go to my notebooks page
    Then notebook "Group E2E Root" should appear at the top level of the notebook catalog

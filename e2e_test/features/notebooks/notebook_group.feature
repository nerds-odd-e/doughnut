Feature: Notebook group

  Background:
    Given I am logged in as an existing user
    And I have a notebook with head note "Group E2E Root" and notes:
      | Title | Parent Title   |
      | child | Group E2E Root |

  Scenario: Move owned notebook to group from catalog
    When I create a notebook group named "Catalog Move Group" from my notebooks page
    And I move notebook "Group E2E Root" to notebook group "Catalog Move Group" from the notebook catalog
    When I go to my notebooks page
    Then I should see notebook group "Catalog Move Group" with a hint including "Group E2E Root"

  Scenario: Move subscribed notebook to group from catalog
    Given I have a notebook with head note "Sub E2E Bazaar" and notes:
      | Title | Parent Title |
      | leaf | Sub E2E Bazaar |
    And notebook "Sub E2E Bazaar" is shared to the Bazaar
    When I subscribe to notebook "Sub E2E Bazaar" in the bazaar, with target of learning 5 notes per day
    When I go to my notebooks page
    And I create a notebook group named "Subscribers Catalog Group" from my notebooks page
    And I move subscribed notebook "Sub E2E Bazaar" to notebook group "Subscribers Catalog Group" from the notebook catalog
    When I go to my notebooks page
    Then I should see notebook group "Subscribers Catalog Group" with a hint including "Sub E2E Bazaar"

  Scenario: Create a group, assign a notebook, see a member hint, then ungroup
    When I create a notebook group named "My E2E Group" from my notebooks page
    And I assign notebook "Group E2E Root" to notebook group "My E2E Group"
    When I go to my notebooks page
    Then I should see notebook group "My E2E Group" with a hint including "Group E2E Root"
    When I open notebook group "My E2E Group" from the catalog header
    Then I should be on the notebook group page for "My E2E Group" with notebook "Group E2E Root" listed
    When I set notebook "Group E2E Root" to ungrouped on the notebook settings page
    When I go to my notebooks page
    Then notebook "Group E2E Root" should appear at the top level of the notebook catalog

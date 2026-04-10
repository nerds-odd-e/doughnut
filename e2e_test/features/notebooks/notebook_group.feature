Feature: Notebook group

  Background:
    Given I am logged in as an existing user
    And I have a notebook with head note "Group E2E Root" and notes:
      | Title | Parent Title   |
      | child | Group E2E Root |

  Scenario: Catalog group for owned notebook, group page, and ungroup
    When I create a notebook group named "E2E Owned Group" by moving owned notebook "Group E2E Root" from the catalog
    Then I should see notebook group "E2E Owned Group" with a hint including "Group E2E Root"
    When I open notebook group "E2E Owned Group" from the catalog header
    Then I should be on the notebook group page for "E2E Owned Group" with notebook "Group E2E Root" listed
    When I set notebook "Group E2E Root" to ungrouped on the notebook settings page
    Then notebook "Group E2E Root" should appear at the top level of the notebook catalog

  Scenario: Catalog group for subscribed notebook
    Given I have a notebook with head note "Sub E2E Bazaar" and notes:
      | Title | Parent Title |
      | leaf | Sub E2E Bazaar |
    And notebook "Sub E2E Bazaar" is shared to the Bazaar
    When I subscribe to notebook "Sub E2E Bazaar" in the bazaar, with target of learning 5 notes per day
    When I create a notebook group named "E2E Subscribed Group" by moving subscribed notebook "Sub E2E Bazaar" from the catalog
    Then I should see notebook group "E2E Subscribed Group" with a hint including "Sub E2E Bazaar"

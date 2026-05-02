Feature: Notebook group

  Background:
    Given I am logged in as an existing user
    And I have a notebook "E2E grouping" with notes:
      | Title |
      | Group E2E Root |
      | child |

  Scenario: Catalog group for owned notebook, group page, and ungroup
    When I create a notebook group named "E2E Owned Group" by moving owned notebook "E2E grouping" from the catalog
    Then I should see notebook group "E2E Owned Group" with a hint including "E2E grouping"
    When I open notebook group "E2E Owned Group" from the catalog header
    Then I should be on the notebook group page for "E2E Owned Group" with notebook "E2E grouping" listed
    When I set notebook "E2E grouping" to ungrouped from the catalog
    Then notebook "E2E grouping" should appear at the top level of the notebook catalog

  Scenario: Catalog group for subscribed notebook
    Given I have a notebook "Bazaar sub fixture" with notes:
      | Title |
      | Sub E2E Bazaar |
      | leaf |
    And notebook "Bazaar sub fixture" is shared to the Bazaar
    When I subscribe to notebook "Bazaar sub fixture" in the bazaar, with target of learning 5 notes per day
    When I create a notebook group named "E2E Subscribed Group" by moving subscribed notebook "Bazaar sub fixture" from the catalog
    Then I should see notebook group "E2E Subscribed Group" with a hint including "Bazaar sub fixture"

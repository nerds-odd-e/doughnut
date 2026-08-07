Feature: Manage Bazaar

  As an admin,
  I want to manage notebooks shared in the Bazaar

  Background:
    Given there are some notes for existing user "another_old_learner" in notebook "Romance"
      | Title  |
      | Sample |
    And there are some notes for existing user "another_old_learner" in notebook "Classics"
      | Title  |
      | Sample |
    And notebook "Romance" is shared to the Bazaar
    And notebook "Classics" is shared to the Bazaar
    And I am logged in as "admin"

  Scenario: Admin removes a notebook from the Bazaar
    When I open the bazaar admin list
    Then the bazaar admin list shows "Romance, Classics"
    When I remove "Romance" from the bazaar admin list
    Then the bazaar admin list shows "Classics"

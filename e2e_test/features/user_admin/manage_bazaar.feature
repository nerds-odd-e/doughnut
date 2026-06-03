Feature: Manage Bazaar

  As an admin,
  I want to manage the notebooks shared in the Bazaar

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

  Scenario: Remove the notebook from Bazaar
    When I navigate to the "Manage Bazaar" section in the admin dashboard
    Then I should see "Romance, Classics" in the bazaar admin list
    When I remove the notebook "Romance" from the bazaar admin list
    Then I should see "Classics" in the bazaar admin list

Feature: Manage Bazaar

  As an admin,
  I want to manage the notebooks shared in the Bazaar

  Background:
    Given I am logged in as "another_old_learner"
    And I have a notebook "Romance"
    And I have a notebook "Classics"
    And notebook "Romance" is shared to the Bazaar
    And notebook "Classics" is shared to the Bazaar
    And I am re-logged in as "admin"

  Scenario: Remove the notebook from Bazaar
    When I navigate to the "Manage Bazaar" section in the admin dashboard
    Then I should see "Romance, Classics" in the bazaar admin list
    When I remove the notebook "Romance" from the bazaar admin list
    Then I should see "Classics" in the bazaar admin list

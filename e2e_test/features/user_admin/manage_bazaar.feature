Feature: Manage Bazaar

  As an admin,
  I want to manage the notebooks shared in the Bazaar

  Background:
    Given I am logged in as "another_old_learner"
    And I have a notebook "Romance" with a note "origin of romance"
    And I have a notebook "Classics" with a note "origin of classics"
    And notebook "Romance" is shared to the Bazaar
    And notebook "Classics" is shared to the Bazaar
    And I am re-logged in as "admin"

  Scenario: Remove the notebook from Bazaar
    * I should see "Romance, Classics" shared in the Bazaar
    When I remove the notebook "Romance" from the bazaar
    Then I should see "Classics" shared in the Bazaar

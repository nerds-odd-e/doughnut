Feature: Manage Bazaar

  As an admin,
  I want to manage the notebooks shared in the Bazaar

  Background:
    Given there are some notes for existing user "another_old_learner"
      | topicConstructor |
      | Romance          |
      | Classics         |
    And notebook "Romance" is shared to the Bazaar
    And notebook "Classsics" is shared to the Bazaar
    And I am logged in as an admin

  @ignore
  Scenario: Remove the notebook from Bazaar
    * I should see "Romance, Classics" is shared in the Bazaar
    When I remove to notebook "Shape" from the bazaar
    Then I should see "Classics" is shared in the Bazaar

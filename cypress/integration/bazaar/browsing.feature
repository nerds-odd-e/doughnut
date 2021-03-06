Feature: Bazaar browsing
  Part of the bazaar should be visible to everyone.

  @ignore
  Scenario: Browsing as non-user
    Given there are some notes for existing user "old_learner"
      | title           | description                | linkTo        |
      | Shape           | The form of something      |               |
      | Square          | four equal straight sides  | Shape         |
      | Triangle        | three sides shape          | Shape         |
    And note "Shape" is shared to the Bazaar
    When I haven't login
    Then I should see "Shape" is shared in the Bazaar


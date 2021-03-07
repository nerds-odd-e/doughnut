Feature: Bazaar browsing
  Part of the bazaar should be visible to everyone.

  Scenario: Browsing as non-user
    Given there are some notes for existing user "old_learner"
      | title           | description                | testingParent |
      | Shape           | The form of something      |               |
      | Square          | four equal straight sides  | Shape         |
      | Triangle        | three sides shape          | Shape         |
    And note "Shape" is shared to the Bazaar
    When I haven't login
    Then I should see "Shape" is shared in the Bazaar
    And there shouldn't be any note edit button for "Shape"
    When I open the note "Shape" in the Bazaar
    Then there shouldn't be any note edit button for "Square"
    And I should see "Bazaar, Shape" in breadcrumb

  Scenario: Breadcrumb should be until the share point

Feature: Bazaar sharing
  There should be a bazaar in Doughnut that people can share their public notes,
  and others can subscribe to their notes.

  Background:
    Given I've logged in as an existing user

  Scenario: Contributing To Bazaar
    Given there are some notes for the current user
      | title           | description                | linkTo        |
      | Shape           | The form of something      |               |
      | Square          | four equal straight sides  | Shape         |
      | Triangle        | three sides shape          | Shape         |
    When I choose to share my note "Shape"
    Then I should see "Shape" is shared in the Bazaar
    #And I can see the children of note "shape" incluing "Square, Triangle" in the Bazaar.


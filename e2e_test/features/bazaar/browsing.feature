Feature: Bazaar browsing
  Part of the bazaar should be visible to everyone.

  Background:
    Given there are some notes for existing user "old_learner"
      | Title            | Details                          | Parent Title|
      | Shape            | The form of something            |             |
      | Rectangle        | four equal straight sides        | Shape       |
      | Triangle         | three sides shape                | Shape       |
      | Square           | a square but big                 | Rectangle   |
      | In OOP           | a square is not a Rectangle      | Rectangle   |
      | interface        | their interfaces are different   | In OOP      |
      | precondition     | square has stronger precondition | In OOP      |
      | Shapes are good  |                                  | Shape       |
    And there is "a specialization of" relationship between note "Square" and "Rectangle"
    And notebook "Shape" is shared to the Bazaar

  Scenario: Browsing as non-user
    When I haven't login
    Then I should see "Shape" shared in the Bazaar
    When I open the notebook "Shape" in the Bazaar
    Then there shouldn't be any note edit button
    And I should see "Bazaar" in breadcrumb
    When I click the child note "Rectangle"
    Then there shouldn't be any note edit button
    And I should see it has relationship to "Square"

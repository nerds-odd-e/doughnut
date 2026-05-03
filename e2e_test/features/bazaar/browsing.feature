Feature: Bazaar browsing
  Part of the bazaar should be visible to everyone.

  Background:
    Given there are some notes for existing user "old_learner" in notebook "Geometry set"
      | Title            | Details                          | Folder              |
      | Shape            | The form of something            |                     |
      | Rectangle        | four equal straight sides        | Topics              |
      | Triangle         | three sides shape                | Topics              |
      | Square           | a square but big                 | Topics/Nested       |
      | In OOP           | a square is not a Rectangle      | Topics/Nested       |
      | interface        | their interfaces are different   | Topics/Nested/Oop   |
      | precondition     | square has stronger precondition | Topics/Nested/Oop   |
      | Shapes are good  |                                  | Topics              |
    And there is "a specialization of" relationship between note "Square" and "Rectangle"
    And notebook "Geometry set" is shared to the Bazaar

  Scenario: Browsing as non-user
    When I haven't login
    Then I should see "Geometry set" shared in the Bazaar
    When I open the notebook "Geometry set" in the Bazaar
    Then there shouldn't be any note edit button
    And I should see "Bazaar" in breadcrumb
    When I navigate to "Bazaar/Geometry set/Topics/Rectangle" note
    Then there shouldn't be any note edit button
    And I should see it has relationship to "Square a specialization of Rectangle"

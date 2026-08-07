Feature: Bazaar browsing
  Part of the bazaar should be visible to everyone.

  Background:
    Given there are some notes for existing user "old_learner" in notebook "Geometry set"
      | Title     | Content                   |
      | Shape     | The form of something     |
      | Rectangle | four equal straight sides |
      | Square    | a square but big          |
    And there is "a specialization of" relationship between note "Square" and "Rectangle" in notebook "Geometry set"
    And notebook "Geometry set" is shared to the Bazaar
    And I haven't login
    And I visit the Bazaar

  Scenario: Non-user sees notebooks shared to the Bazaar
    Then I should see "Geometry set" shared in the Bazaar

  Scenario: Non-user browses a shared notebook as read-only
    When I open the notebook "Geometry set" in the Bazaar
    Then I should not be able to edit the notes
    And I should see "Bazaar" in breadcrumb
    When I open the note "Rectangle" from the sidebar
    Then I should not be able to edit the notes

  Scenario: Non-user can see relationships in shared notes
    When I open the notebook "Geometry set" in the Bazaar
    And I open the note "Rectangle" from the sidebar
    Then I should see it has relationship to "Square a specialization of Rectangle"

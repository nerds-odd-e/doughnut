Feature: Bazaar subscription
  As a learner, I want to subscribe to notes in the Bazaar so that I can
  learn it's content.

  Background:
    Given there are some notes for existing user "another_old_learner"
      | topicConstructor | testingParent |
      | Shape            |               |
      | Square           | Shape         |
      | Triangle         | Shape         |
      | Big Square       | Square        |
    And notebook "Shape" is shared to the Bazaar

  @ignore
  Scenario: subscribe to a note and browse
    Given I am logged in as an existing user
    When I subscribe to notebook "Shape" in the bazaar, with target of learning 1 notes per day
    Then I should see I've subscribed to "Shape"
    And I should see readonly notebook "Shape" in my notes
    When I unsubscribe from notebook "Shape"
    Then I should see I've not subscribed to "Shape"

  @mockBrowserTime
  Scenario: subscribe to a note and review
    Given I am logged in as an existing user
    And I have a note with the topic "My memo"
    And my daily new notes to review is set to 2
    When I subscribe to notebook "Shape" in the bazaar, with target of learning 1 notes per day
    Then On day 1 I should have "2/4" note for initial review and "0/0" for repeat
    And  On day 1 I repeat old "                     " and initial review new "Square, My memo, end"
    And  On day 2 I repeat old "Square, My memo, end " and initial review new "Triangle, end       "
    And  I should be able to edit the subscription to notebook "Shape"

  Scenario: No "add to learning" button for skip-review notebook
    Given I am logged in as "another_old_learner"
    When I change notebook "Shape" to skip review
    Then I go to the bazaar
    And I should not see the "Add to my learning" button on notebook "Shape"


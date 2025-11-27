@usingMockedOpenAiService
Feature: Bazaar subscription
  As a learner, I want to subscribe to notes in the Bazaar so that I can
  learn it's content.

  Background:
    Given there are some notes for existing user "another_old_learner"
      | Title            | Parent Title| Skip Memory Tracking|
      | Shape            |             | true       |
      | Square           | Shape       |            |
      | Triangle         | Shape       |            |
      | Big Square       | Square      |            |
    And notebook "Shape" is shared to the Bazaar

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
    And I have a notebook with the head note "My memo"
    And my daily new notes to review is set to 2
    When I subscribe to notebook "Shape" in the bazaar, with target of learning 1 notes per day
    And the OpenAI service is unavailable due to invalid system token
    Then On day 1 I should have "0/2/4" note for assimilation and "0/0/0" for recall
    And  On day 1 I recall "                     " and initial review new "Square, My memo, end"
    And  On day 2 I recall "Square, My memo, end " and initial review new "Triangle, end       "
    And  I should be able to edit the subscription to notebook "Shape"

  Scenario: No "add to learning" button for notebook with skip memory tracking
    Given I am logged in as "another_old_learner"
    When I change notebook "Shape" to skip review
    Then I can not see add the notebook "Shape" to my learning in the bazaar


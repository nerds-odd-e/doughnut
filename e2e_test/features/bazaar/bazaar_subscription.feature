@usingMockedOpenAiService
Feature: Bazaar subscription
  As a learner, I want to subscribe to notes in the Bazaar so that I can
  assimilate and recall its content.

  Background:
    Given there are some notes for existing user "another_old_learner" in notebook "Shape"
      | Title      |
      | Shape      |
      | Square     |
      | Triangle   |
      | Big Square |
    And I have a session as "another_old_learner"
    And the notes "Shape" are skipped from the assimilation sequence
    And my session is logged out
    And notebook "Shape" is shared to the Bazaar

  @skipOptimizationDueToKnownNecessarySlowness
  Scenario: Subscribe and unsubscribe from a Bazaar notebook
    Given I am logged in as an existing user
    When I subscribe to notebook "Shape" in the bazaar, with daily assimilation target of 1 notes per day
    Then I should see I've subscribed to "Shape"
    And I should see readonly notebook "Shape" in the notebook catalog
    When I unsubscribe from notebook "Shape"
    Then I should see I've not subscribed to "Shape"

  @mockBrowserTime
  Scenario: Assimilate notes from a Bazaar subscription
    Given I am logged in as an existing user
    And I have a notebook "Memo pad" with a note "My memo"
    And my daily new notes to assimilate is set to 2
    And I have subscribed to notebook "Shape" in the bazaar with daily assimilation target of 1
    Then On day 1 I should have "0/2/4" note for assimilation and "0/0/0" for recall
    And the notes "Square, My memo" are assimilated on day 1
    Then On day 2 I should have "0/2/2" note for assimilation and "0/2/2" for recall

  Scenario: Notebook with Skip Memory Tracking cannot be subscribed from the Bazaar
    Given I am logged in as "another_old_learner"
    When I change notebook "Shape" to skip memory tracking
    Then I should not be able to subscribe to notebook "Shape" from the Bazaar

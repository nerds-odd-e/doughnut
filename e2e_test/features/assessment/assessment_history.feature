Feature: View my assessment history
  As a learner, I want to view my past assessment results.

  Background:
    Given I am logged in as "old_learner"
    And there is a notebook "Just say 'Yes'" by "a_trainer" with 2 questions, shared to the Bazaar

  Scenario Outline: Viewing assessment results in history
    When I achieve a score of <Score> in the assessment of the notebook "<Notebook>"
    Then I should see the result "<Result>" for the notebook "<Notebook>" in my assessment history

    Examples:
      | Notebook           | Score | Result |
      | Just say 'Yes'     | 2/2   | Pass   |
      | Just say 'Yes'     | 1/2   | Fail   |

Feature: View my assessment and certificate history
  As a learner, I want to view my past assessments and any certificates I have earned.

  Background:
    Given I am logged in as "old_learner"
    And there is a notebook "Just say 'Yes'" by "a_trainer" with 2 questions, shared to the Bazaar
    And there is a certified notebook "Say 'Yes' Professionally" by "a_trainer" with 2 questions, shared to the Bazaar

  Scenario Outline: Viewing assessment results and certificate availability
    When I achieve a score of <Score> in the assessment of the notebook "<Notebook>"
    Then I should see the result "<Result>" for the notebook "<Notebook>" in my assessment and certificate history
    And  I <Can Or Cannot> view the certificate for the notebook "<Notebook>" in my assessment and certificate history

    Examples:
      | Notebook                 | Score | Result | Can Or Cannot |
      # | Just say 'Yes'           | 2/2   | Pass   | cannot        |
      | Say 'Yes' Professionally | 2/2   | Pass   | can           |
      | Just say 'Yes'           | 1/2   | Fail   | cannot        |

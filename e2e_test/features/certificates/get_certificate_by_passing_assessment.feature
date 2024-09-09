Feature: Get certificate by an assessment
  As a trainer, I want to provide certificates to learners who pass assessments,
  so that they can demonstrate their skill level on the topic.

  As a learner, I want to obtain a certificate when I pass an assessment,
  so that I can showcase my expertise.

  Background:
    Given I am logged in as "old_learner"
    And there is a certified notebook "Just say 'Yes'" by "a_trainer" with 2 questions, shared to the Bazaar

  Scenario Outline: I should pass the assessment when I achieve a score of more than 80%
    When I achieve a score of <Score> in the assessment of the notebook "Just say 'Yes'"
    Then I should <Pass or not> the assessment of "Just say 'Yes'"
    And I should <Get a certificate or not> of "Just say 'Yes'" for "Old Learner" from "A Trainer"

    Examples:
      | Score | Pass or not | Get a certificate or not |
      | 2/2   | pass        | get a certificate        |
      | 1/2   | not pass    | not get a certificate    |
      | 0/2   | not pass    | not get a certificate    |

  Scenario: I should see the original start date on my renewed certificate
    Given the current date is "2021-08-09"
    And I achieve a score of 2/2 in the assessment of the notebook "Just say 'Yes'"
    When the current date is "2024-08-09"
    And I achieve a score of 2/2 in the assessment of the notebook "Just say 'Yes'"
    Then I should see the original start date "2021-08-09" on my renewed certificate for "Just say 'Yes'"

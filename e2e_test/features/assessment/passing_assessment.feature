Feature: Get certificate by an assessment
  As a trainer, I want to provide certificate to the learner when they pass the assessment,
  so that they can use it to show their skill level on the topic.

  As a learner, I want to obtain a certificate when I pass the assessment.

  Background:
    Given I am logged in as "old_learner"
    And there is an assessment on notebook "Just say 'Yes'" with 2 questions

  Scenario Outline: I should pass the assessment when I get score more than 80%
    Given I request for an approval for notebooks:
      | Just say 'Yes'  |
    And I am logged in as an admin
    And I approve notebook "Just say 'Yes'"
    And I logout via the UI
    And I am logged in as "old_learner"
    When I get score <Score> when do the assessment on "Just say 'Yes'"
    Then I should <Pass or not> the assessment of "Just say 'Yes'"
    And I should <Get a certificate or not> of "Just say 'Yes'" for "Old Learner" from "Old Learner"

    Examples:
      | Score | Pass or not | Get a certificate or not |
      | 2/2   | pass        | get a certificate        |
      | 1/2   | not pass    | not get a certificate    |
      | 0/2   | not pass    | not get a certificate    |
  @skip
  Scenario: I cannot receive a certificate on passing an unapproved assessment
  # Given the notebook "Just say 'Yes'" is unapproved
  When I pass the assessment on "Just say 'Yes'"
  Then I cannot download a certificate after passing an assessment

  @ignore
  Scenario: I should see the original start date on my renewed certificate
    Given Now is "2021-08-09"
    And I get score 2/2 when do the assessment on "Just say 'Yes'"
    When Now is "2024-08-09"
    And I get score 2/2 when do the assessment on "Just say 'Yes'"
    Then I should see the original start date "2021-08-09" on my renewed certificate for "Just say 'Yes'"

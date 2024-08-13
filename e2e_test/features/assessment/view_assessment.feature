@ignore
Feature: View my assessment history
  As a learner, I want to be able to view my past assessment

  Background:
    Given I am logged in as an existing user

  Scenario: Have not taken any assessment
    When I try to view my assessment history
    Then I should see my assessment history with empty records

  Scenario: Have taken at least one assessment of a notebook
    When I try to view my assessment history
    Then I should see one record of the assessment

  Scenario Outline: Have attempted one assessment of a notebook
    When I view my assessment history
    Then I should see <Notebook> result as <Result>

    Examples:
      | Notebook | Result |
      | 1        | Pass   |
      | 2        | Fail   |




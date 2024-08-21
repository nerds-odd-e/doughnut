Feature: View my assessment history
  As a learner, I want to be able to view my past assessment

  Background:
    Given I am logged in as an existing user
    And there is an assessment on notebook "Just say 'Yes'" with 2 questions

  Scenario: Have not taken any assessment
    Then I should see my assessment history with empty records

  Scenario Outline: Have attempted assessment of a notebook
    When I get score <Score> when do the assessment on "Just say 'Yes'"
    Then I should see "Just say 'Yes'" result as "<Result>" in my assessment history

    Examples:
      | Score | Result |
      | 2/2   | Pass   |
      | 1/2   | Fail   |

  Scenario: I can view certificate for passed assessment
    When I get score 2/2 when do the assessment on "Just say 'Yes'"
    Then I can view certificate of "Just say 'Yes'" in my assessment history




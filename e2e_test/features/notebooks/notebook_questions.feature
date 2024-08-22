
Feature: Notebook questions

  Background:
    Given I am logged in as an existing user
    And I have a notebook with head note "LeSS in Action" and notes:
      | Topic | Parent Topic   |
      | tech  | LeSS in Action |
      | management  | LeSS in Action |
      | team  | management           |

  Scenario: I have a notebook with no questions
    Then I should see that there are no questions for "LeSS in Action" for the following topics:
      | Topic |
      | management  |
  @wip 
  @skip
  Scenario: View all questions in a notebook
    When I add questions to the following notes in the notebook "LeSS in Action"
      | Topic | Question                | Answer                |
      | team  | Who is the team?        | The team is ...       |
      | tech  | What is the technology? | The technology is ... |
    Then I should see the following questions for the topics:
      | Topic | Question                | Answer                |
      | team  | Who is the team?        | The team is ...       |
      | tech  | What is the technology? | The technology is ... |
    And I should see that there are no questions for the following topics:
      | Topic |
      | management  |

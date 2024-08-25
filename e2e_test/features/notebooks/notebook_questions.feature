
Feature: Notebook questions
  As a trainer, I want to see all my questions in a notebook in one place,
  so that I can review them before sharing the notebook with others.

  Background:
    Given I am logged in as an existing user
    And I have a notebook with head note "LeSS in Action" and notes:
      | Topic | Parent Topic   |
      | tech  | LeSS in Action |
      | management  | LeSS in Action |
      | team  | management           |

  Scenario: View all questions in a notebook
    When I add questions to the following notes in the notebook "LeSS in Action"
      | Topic | Question                |
      | team  | Who is the team?        |
      | tech  | What is the technology? |
    Then I should see the following questions for the topics in the notebook "LeSS in Action":
      | Topic | Question                |
      | team  | Who is the team?        |
      | tech  | What is the technology? |
    And I should see that there are no questions for "LeSS in Action" for the following topics:
      | Topic |
      | management  |

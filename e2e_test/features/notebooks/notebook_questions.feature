
Feature: Notebook questions

  Background:
    Given I am logged in as an existing user
    And I have a notebook with head note "LeSS in Action" and notes:
      | Title | Parent Title   |
      | tech  | LeSS in Action |
      | management  | LeSS in Action |
      | team  | management           |

  Scenario: View all questions in a notebook
    When I add questions to the following notes in the notebook "LeSS in Action"
      | Title | Question                |
      | team  | Who is the team?        |
      | tech  | What is the technology? |
    Then I should see the following questions for the topics in the notebook "LeSS in Action":
      | Title | Question                |
      | team  | Who is the team?        |
      | tech  | What is the technology? |
    And I should see that there are no questions for "LeSS in Action" for the following topics:
      | Title |
      | management  |

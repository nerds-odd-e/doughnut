@ignore
Feature: Get approved questions
  As a learner, I want to do assessment for a notebook with only approved questions.

  Background:
    Given I am logged in as an existing user
  Scenario: Generating an Assessment with Random Approved Questions
    Given there are some notes for the current user:
      | topicConstructor | testingParent |
      | Animals          |               |
      | Zebra            | Animals       |
      | Flamingo         | Animals       |
      | Elephant         | Animals       |
      | Kitty cat        | Animals       |
      | Merlion          | Animals       |
      | Unicorn          | Animals       |
    And there are questions for the notes:
      | Question            | Note      | Status   |
      | Zebra Question 1    | Zebra     | Approved |
      | Flamingo Question 1 | Flamingo  | Approved |
      | Elephant Question 1 | Elephant  | Approved |
      | Cat Question 1      | Kitty cat | Approved |
      | Merlion Question 1  | Merlion   | Approved |
      | Unicorn Question 1  | Unicorn   | Approved |
    When an assessment is generated
    Then the assessment should include 5 randomly selected approved questions
    And each question should pertain to a different note

  Scenario: Generating an Assessment with Insufficient Approved Questions
    Given that there are less than 5 approved questions in the notebook titled "Shape"
    And these questions are assigned to different notes
    When an assessment is generated
    Then the system should notify about the insufficient number of questions

  Scenario: Generating an Assessment with More than 5 Approved Questions
    Given that there are more than 5 approved questions in the notebook titled "Shape"
    And these questions are assigned to different notes
    When an assessment is generated
    Then the assessment should include only 5 randomly selected approved questions

  Scenario: Generating an Assessment with only Unapproved Questions
    Given that there are 5 unapproved questions in the notebook titled "Shape"
    And these questions are assigned to different notes
    When an assessment is generated
    Then the system should notify about the insufficient number of questions

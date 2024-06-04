@ignore
Feature: Get approved questions
  As a learner, I want to do assessment for a notebook with only approved questions.

  Background:
    Given I am logged in as an existing user
    And notebook "Shape" is shared to the Bazaar

  Scenario: Generating an Assessment with Random Approved Questions
    Given that there are 5 approved questions in the notebook titled "Shape"
    And these questions are assigned to 5 different notes
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

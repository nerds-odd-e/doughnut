@ignore
Feature: New questions assessment

  Background:
    Given I am logged in as an existing user
    And there is a notebook in the bazaar
    And there are notes in the notebook

  Scenario: Start an assessment with new questions
    When I do the assessment
    Then I should see 5 questions and the mcq questions have 4 options each


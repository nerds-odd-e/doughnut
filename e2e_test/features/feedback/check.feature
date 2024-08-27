@ignore
Feature: Response to the feedback message
  As a learner, I want to respond to feedback.

  Background:
    Given I am logged in as an existing user

  Scenario: Provide response to feedback
    Given I visit the feedback overview page
    When I have recieved feedback on a question
    And I open a feedback chat
    Then I should be able to respond to the feedback

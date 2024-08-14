@ignore
Feature: As a learner, I want to be able to get my certificate on assessments which I have passed

  Background:
    Given I am logged in as an existing user
    And there is an assessment on notebook "Just say 'Yes'" with 2 questions

  Scenario: I have passed the assessment
    When I click on get certificate button
    Then I should get my assessment certificate

  Scenario: I have not passed the assessment
    When I view assessment history page
    Then I should not be able to get my assessment certificate

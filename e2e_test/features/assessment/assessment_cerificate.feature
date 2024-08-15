Feature: As a learner, I want to be able to get my certificate on assessments which I have passed

  Background:
    Given I am logged in as an existing user
    And I have completed the assessment on notebook "LeSS in Action" with 2 questions

  Scenario: I have passed the assessment
    When I view my assessment history of topic "LeSS in Action" and scored 2/2 on the assessment
    Then I should be able to click the 'Get Certificate' button to get my assessment certificate

  Scenario: I have not passed the assessment
    When I view my assessment history of topic "LeSS in Action" and scored 1/2 on the assessment
    Then I should not be able to click on the 'Get Certificate' button

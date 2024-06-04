Feature: New questions assessment

  Background:
    Given I am logged in as an existing user
    And there are some notes for the current user:
      | topicConstructor | testingParent |
      | Shape            |               |
      | Triangle         | Shape         |
  Scenario: Start an assessment with new questions
    When I do the assessment on "My Notes/Shape"
    Then I should see 5 questions and the mcq questions have 4 options each


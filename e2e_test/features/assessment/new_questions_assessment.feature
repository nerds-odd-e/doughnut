Feature: New questions assessment

  Background:
    Given I am logged in as an existing user
    And there are some notes for the current user:
      | topicConstructor | testingParent |
      | Shape            |               |
      | Triangle         | Shape         |
  @ignore
  Scenario: Start an assessment
    When I click the assessment launch button on "My Notes/Shape"
    Then I should see the assessment modal launched

#  Scenario: Start an assessment with new questions
#    When I do the assessment on "My Notes/Shape"
#    Then I should see the first question and it should have 4 options


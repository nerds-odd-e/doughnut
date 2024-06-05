Feature: New questions assessment

  Background:
    Given I am logged in as an existing user
    And there are some notes for the current user:
      | topicConstructor | testingParent |
      | Shape            |               |
      | Triangle         | Shape         |

  Scenario: Start an assessment
    Given I start the assessment on "My Notes/Shape" notebook
    When I answer the first question
    # Then I should see the next question\

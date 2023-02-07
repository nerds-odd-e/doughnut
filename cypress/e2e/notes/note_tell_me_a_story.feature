Feature: Tell me a story
  As a learner, I want to the notes to be summarized like a story

  Background:
    Given I've logged in as an existing user
    And there are some notes for the current user
      | title    | description         | testingParent |
      | Animals  | Are living beings   |               |
    And OpenAI thinks that "Animals" means "Sharing the same planet as humans"

  @ignore
  @usingMockedOpenAiService
  Scenario: Click the Story button creates a modal message
    When I ask for a story for "Animals"
    Then I should be prompted with a story description "Sharing the same planet as humans"



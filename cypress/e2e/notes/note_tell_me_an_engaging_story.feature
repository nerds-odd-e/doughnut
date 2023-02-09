Feature: Tell me a engaging story
  As a learner, I want to the notes to be summarized like an engaging story

  Background:
    Given I've logged in as an existing user
    And there are some notes for the current user
      | title    | description         |
      | Animals  | Are living beings   |

  @usingMockedOpenAiService
  Scenario: Click the Engaging Story button creates a modal message
    Given OpenAI thinks that "Animals" means "Cow says moo"
    When I ask for an engaging story for "Animals"
    Then I should be prompted with an engaging story description "Cow says moo"

  @ignore
  @usingMockedOpenAiService
  Scenario: Show error message when things don't work
    Given An OpenAI response is unavailable
    When I ask for an engaging story for "Animals"
    Then I should be prompted with an error message saying "There is a problem"


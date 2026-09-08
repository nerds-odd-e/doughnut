@usingMockedOpenAiService
Feature: Note content completion
  As a learner, I want to complete note content with OpenAI so I can finish drafting faster.

  Background:
    Given I am logged in as an existing user
    And I have a notebook "Taiwan primer" with notes:
      | Title  | Content |
      | Taipei | It is a |

  @openaiUnavailableWhenPaired
  Scenario: Content completion fails when OpenAI is unavailable
    Given the OpenAI service is unavailable due to invalid system token
    When I request to complete the content for the note "Taipei"
    Then I should see a notification of a bad request

  Scenario: Accepting a suggested note content completion
    Given OpenAI will reply below for user messages:
      | user message                      | response type   | assistant reply                       |
      | Please complete the note content. | requires action | {"content": "It is a vigorous city."} |
    When I request to complete the content for the note "Taipei"
    Then I should see the suggested completion
    And my OpenAI mock still records only my request marker
    And I keep my OpenAI mock configured across the other worktree's mock reset
    Then my OpenAI mock still records only my request marker
    When I accept the suggested completion
    Then the note content on the current page should be this run's OpenAI mock suggestion

@usingMockedOpenAiService
Feature: Reviewing generated note content
  As a note author, I want to inspect and edit a generated correction in its conversation.

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

  Scenario: Reviewing and accepting a note content correction
    Given OpenAI will reply below for user messages:
      | user message                      | response type   | assistant reply                       |
      | Please complete the note content. | requires action | {"content": "It is a vigorous city.", "reason": "Completes the city description."} |
    When I request to complete the content for the note "Taipei"
    Then I should see this run's pending content correction
    And my OpenAI mock still records only my request marker
    And I keep my OpenAI mock configured across the other worktree's mock reset
    Then my OpenAI mock still records only my request marker
    When I revise the pending content correction to "Taipei is a lively city."
    Then the pending content correction should contain "Taipei is a lively city."
    When I close the conversation
    Then the note content on the current page should be "It is a"
    When I start a conversation about this note
    Then I should see this run's pending content correction
    When I revise the pending content correction to "Taipei is a lively city."
    And I accept the reviewed content correction
    Then the accepted content correction should retain "Taipei is a lively city."
    When I open the corrected note
    Then the note content on the current page should be "Taipei is a lively city."
    When I start a conversation about this note
    Then the accepted content correction should retain "Taipei is a lively city."

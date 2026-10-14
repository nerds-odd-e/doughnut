@usingMockedOpenAiService
Feature: Conversation about a note with AI
  As a learner, I want to start a conversation with AI about a note,
  so that I can understand the note better.

  Background:
    Given I am logged in as an existing user
    And I have a notebook "Japan facts" with a note "There are 42 prefectures in Japan"

  Scenario: Ask AI about a note, follow up, and export the conversation
    Given OpenAI will reply below for user messages:
      | user message          | assistant reply              |
      | Is Naba one of them?  | No. It is not.               |
      | Is this note correct? | No, there are 47 prefectures |

    When I start a conversation about the note "There are 42 prefectures in Japan" inviting AI with the message "Is Naba one of them?"
    Then I should see the following messages:
      | role      | message              |
      | user      | Is Naba one of them? |
      | assistant | No. It is not.       |
    And OpenAI responses were called with Donut focus context
    When I send the message "Is this note correct?" to AI
    Then I should see the following messages:
      | role      | message                      |
      | user      | Is this note correct?        |
      | assistant | No, there are 47 prefectures |
    When I export the conversation
    Then the export should contain the user message "Is Naba one of them?"
    And the export should contain the assistant reply "No. It is not."
    And I should be able to copy the export

  Scenario: An author accepts an edited correction and reopens its confirmed result
    Given OpenAI will reply below for user messages:
      | user message | response type | assistant reply |
      | Clarify the prefecture count. | requires action | {"content":"Japan has 47 prefectures.","reason":"Corrects the prefecture count."} |
    When I start a conversation about the note "There are 42 prefectures in Japan" inviting AI with the message "Clarify the prefecture count."
    And I revise the pending content correction to "Japan has 47 prefectures, including Tokyo."
    And I accept the reviewed content correction
    Then the accepted content correction should retain "Japan has 47 prefectures, including Tokyo."
    When I open the corrected note
    Then the note content on the current page should be "Japan has 47 prefectures, including Tokyo."
    When I start a conversation about this note
    Then the accepted content correction should retain "Japan has 47 prefectures, including Tokyo."

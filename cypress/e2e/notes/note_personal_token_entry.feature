Feature: Personal token entry for Open AI functionality
  As a learner, I want to use my personal token to utilize the OpenAI features for getting suggestions on this note

  Background:
    Given I've logged in as an existing user
    And there are some notes for the current user
      | title          | description    |
      | Flowers        | Something else |
    And OpenAI thinks that "Flowers" means "Flowers are beautiful creations of the earth"

    @usingMockedOpenAiService
    Scenario Outline: Get suggestions with open AI personal or fallback token
      Given the fallback key of the system is "<fallback key>"
      And I have a personal openAI token "<personal key>"
      When I ask for a description suggestion for "Flowers"
      Then I <result> get a suggestion "Flowers are beautiful creations of the earth"

      Examples:
     | fallback key | personal key | result  |
     | valid key    | valid key    | should    |
     #| invalid key  | valid key    | should    |
     #| valid key    | invalid      | should    |
     | invalid      | invalid      | should not |


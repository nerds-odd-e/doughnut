Feature: Open AI service availability
  As a learner, I want to use my personal token to utilize the OpenAI features for getting suggestions on this note

  Background:
    Given I've logged in as an existing user
    And there are some notes for the current user
      | title          |
      | Flowers        |

    @usingMockedOpenAiService
    Scenario: Open AI service availability
      Given open AI serivce always think the system token is invalid
      When I ask for a description suggestion for "Flowers"
      Then I should see that the open AI service is not available

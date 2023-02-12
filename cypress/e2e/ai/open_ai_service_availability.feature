Feature: Open AI service availability
  As the system manager, I want the user get informed when
  the Open AI service is not available, so that they can
  still use the system without it.

  Background:
    Given I've logged in as an existing user
    And I have a note with the title "Flowers"

    @usingMockedOpenAiService
    Scenario: Open AI service availability
      Given open AI serivce always think the system token is invalid
      When I ask for a description suggestion for "Flowers"
      Then I should see that the open AI service is not available

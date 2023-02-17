Feature: Open AI service availability
  As the system manager, I want the user get informed when
  the Open AI service is not available, so that they can
  still use the system without it.

  Background:
    Given I've logged in as an existing user
    And I have a note with the title "Gravity"

    @usingMockedOpenAiService
    Scenario: Open AI service availability
      Given open AI serivce always think the system token is invalid
      When I ask for a description suggestion for "Gravity"
      Then I should see that the open AI service is not available

  @usingMockedOpenAiService
  Scenario: Suggestions parts are displayed when failed to get full response
    Given OpenAI has an incomplete idea that "Gravity" means "What goes up"
    When I ask for a description suggestion for "Gravity"
    Then I should be prompted with a suggested description "What goes up"

  # this test is not possible to implement with the current implementation of Cypress.
  # because the fetch request will block until the full response is received.
  @usingMockedOpenAiService
  Scenario: Suggestions parts are displayed as soon as they are available

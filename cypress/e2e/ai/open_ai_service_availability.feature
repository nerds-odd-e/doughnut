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
  Scenario: Suggestions parts are displayed as soon as they are available
    Given OpenAI has an incomplete idea that "Gravity" means "What goes up"
    When I ask for a description suggestion for "Gravity"
    Then I should be prompted with a suggested description "What goes up"
    When OpenAI thinks that "Gravity" means "What goes up must come down"
    # Then I should be prompted with a suggested description "What goes up must come down"

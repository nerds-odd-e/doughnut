Feature: Note Translation
  As a user, I want to be able to insert my translation of title and description of a note. and also showing it in the note.

  Background:
    Given I've logged in as an existing user

  @ignore
  Scenario: Showing fallback translation when no translation is available
    Given there existing note for the current user
      | title                                   | description                                                                                                                                                                                                 |
      | Potentially shippable product increment | The output of every Sprint is called a Potentially Shippable Product Increment. The work of all the teams must be integrated before the end of every Sprint—the integration must be done during the Sprint. |
    When I click Indonesian flag icon 
    And no description translation available
    Then I should see the warning text "No translation available" below the description text

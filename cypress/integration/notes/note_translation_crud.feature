Feature: Note Translation Crud
  As a user, I want to be able to insert or update my translation of title and description of a note through a form page.

  Background:
    Given I've logged in as an existing user

  @ignore
  Scenario: Showing input translation button on notes page
    Given there existing note for the current user
      | title                                   | description                                                                                                                                                                                                 |
      | Potentially shippable product increment | The output of every Sprint is called a Potentially Shippable Product Increment. The work of all the teams must be integrated before the end of every Sprint—the integration must be done during the Sprint. |
    Then I should see the button with text 'Input Translation'
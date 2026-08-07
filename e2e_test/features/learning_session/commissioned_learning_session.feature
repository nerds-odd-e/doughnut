@disableOpenAiService @mockBrowserTime
Feature: Commissioned learning session
  As a learner, I want to assimilate a note as a commissioned memory tracker
  so that a tutor can later conduct a learning session for it.

  Background:
    Given I am logged in as an existing user
    And my space setting is "1, 2, 4, 8"
    And I have a notebook "Spanish conversation" with notes:
      | Title   | Content   |
      | Hola    | Hello     |
      | Gracias | Thank you |
    And It's day 1, 8 hour

  Scenario: Assimilating a note with a tutor creates a commissioned memory tracker
    When I am assimilating the note "Hola"
    And I assimilate it as commissioned
    And I open assimilation settings
    Then I should see a commissioned memory tracker for "Hola"

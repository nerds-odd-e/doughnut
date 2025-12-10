Feature: Summary Generation
    As a learner, I want to generate a summary of my note using AI

  Background:
    Given I am logged in as an existing user
@ignore
  Scenario: Generate a summary of a note
    Given there is a note "English" with details "English is a language that is spoken in many countries. It is also the most widely spoken language in the world."
    When I start assimilating "English"
    Then I should see a summary of the note broken down into two points: "English is a language that is spoken in many countries." and "It is also the most widely spoken language in the world."
Feature: Summary Generation
    As a learner, I want to generate a summary of my note using AI

  Background:
    Given I am logged in as an existing user
    And I have a notebook with the head note "English" and details "English is a language that is spoken in many countries. It is also the most widely spoken language in the world."

  Scenario: Generate a summary of a note
    When I start assimilating "English"
    Then I should see a summary of the note broken down into two points: "English is a language that is spoken in many countries." and "It is also the most widely spoken language in the world."


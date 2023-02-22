Feature: Note Edit Accessories
  As a learner, I want to be able to edit the Accessories of a note

  Background:
    Given I've logged in as an existing user
    And I have a note with the title "lunar"

  Scenario: Edit a note
    When I update note accessories of "lunar" to become:
      | Url               |
      | https://moon.com  |
    And I update note accessories of "lunar" to become:
      | Upload Picture  |
      | moon.jpg        |
    Then I should see note "lunar" has a picture and a url "https://moon.com"

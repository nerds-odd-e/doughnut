Feature: Note Edit Accessories
  As a learner, I want to be able to edit the Accessories of a note

  Background:
    Given I am logged in as an existing user
    And I have a notebook with the head note "lunar"

  Scenario: Edit a note
    When I update note accessories of "lunar" to become:
      | Url               | Upload Image  |
      | https://moon.com  | moon.jpg        |
    Then I should see note "lunar" has a image and a url "https://moon.com"

  Scenario: Edit a note with Image url
    When I update note accessories of "lunar" to become:
      | Url               | Image Url     |
      | https://moon.com  | moon.jpg        |
    Then I should see note "lunar" has a image and a url "https://moon.com"

Feature: link types

  Background:
    Given I've logged in as an existing user
    And there are some notes for the current user
      | title                      | skipReview |
      | Solar system               | true       |
      | Earth-Moon system          | true       |
      | Earth                      | true       |
      | Mars                       | true       |
      | Moon                       | true       |
      | Astronomical object        | true       |
      | planet                     | true       |
      | moon (astronomical object) | true       |
    And there is "a specialization of" link between note "planet" and "Astronomical object"
    And there is "a specialization of" link between note "moon (astronomical object)" and "Astronomical object"
    And there is "an example of" link between note "Moon" and "moon (astronomical object)"
    And there is "an example of" link between note "Earth" and "planet"
    And there is "an example of" link between note "Mars" and "planet"

  Scenario: Link inherit
    When I visit note "Moon"
    Then I should see the note "is part of" "Solar system/Earth-Moon system"


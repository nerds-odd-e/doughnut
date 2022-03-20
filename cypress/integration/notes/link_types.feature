Feature: link types

  Background:
    Given I've logged in as an existing user
    And there are some notes for the current user
      | title             | skipReview |
      | Solar system      | true       |
      | Earth-Moon system | true       |
      | Earth             | true       |
      | Moon              | true       |
    And there is "a part of" link between note "Earth-Moon system" and "Solar system"
    And there is "a part of" link between note "Moon" and "Earth-Moon system"
    And there is "a part of" link between note "Earth" and "Earth-Moon system"

  Scenario: Link inherit
    When I visit note "Moon"
    Then I should see the note "is part of" "Solar system/Earth-Moon system"


Feature: link note

  @ignore
  Scenario: Link two notes together
    Given I have created two notes
      | title      |
      | Sedition   |
      | Sedation   |
    When I select the note Sedition
    And I create link
    And I select Sedation note as target
    Then I should see the link created between note Sedition and Sedation

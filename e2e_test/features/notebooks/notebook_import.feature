Feature: Notebook Import
  As a user
  I want to import my Obsidian notes into Doughnut
  So that I can migrate my existing notes

  Background:
    Given I am logged in as an existing user
    And I have a notebook titled "note 1"

  @ignore
  Scenario: Import notes from Obsidian
    When I Import Obsidian data "import-one-child.zip" to note "note 1"
    Then I should see "note 1" with these children
      | note-title|
      | note 2    |

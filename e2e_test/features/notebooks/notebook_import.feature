Feature: Notebook Import
  As a user
  I want to import my Obsidian notes into Doughnut
  So that I can migrate my existing notes

  Background:
    Given I am logged in as an existing user
    And I have a notebook with head note "title 1" and notes:
      | Title   | Details | Parent Title |
      | note 1  |         | title 1      |

  
  Scenario: Import zip from Obsidian with one new child
    When I Import Obsidian data "import-one-child.zip" to note "title 1"
    Then I should see "title 1/note 1" with these children
      | note-title |
      | note 2   |

  @ignore
  Scenario: Import zip from Obsidian with two nested new child
    When I Import Obsidian data "import-two-layer.zip" to note "title 1"
    Then I should see "title 1/note 1/note 2" with these children
      | note-title |
      | note 3   |
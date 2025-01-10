Feature: Notebook Import
  As a user
  I want to import my Obsidian notes into Doughnut
  So that I can migrate my existing notes

  Background:
    Given I am logged in as an existing user


  Scenario: Import zip from Obsidian with one new child
    Given  I have a notebook with head note "title 1" and notes:
      | Title   | Details | Parent Title |
      | note 1  |         | title 1      |
    When I Import Obsidian data "import-one-child.zip" to note "title 1"
    Then I should see "title 1/note 1" with these children
      | note-title |
      | note 2     |
    And I should see note "title 1/note 1/note 2" has details "content of note 2"

  @ignore
  Scenario: Import zip from Obsidian with one new child
    Given  I have a notebook with head note "title 1" and notes:
      | Title   | Details | Parent Title |
      | note 1  |         | title 1      |
    When I Import Obsidian data "import-directory-only.zip" to note "title 1"
    Then I should see "title 1/note 1" with these children
      | note-title |
      | note 2     |
    And I should see note "title 1/note 1/note 2" has details ""

  Scenario: Import zip from Obsidian with two nested new child
    Given I have a notebook with head note "title 1" and notes:
      | Title   | Details | Parent Title |
      | note 1  |         | title 1      |
      | note 2  |         | note 1       |
    When I Import Obsidian data "import-two-layer.zip" to note "title 1"
    Then I should see "title 1/note 1/note 2" with these children
      | note-title |
      | note 3     |
    And I should see note "title 1/note 1/note 2/note 3" has details "content of note 3"

  @ignore
  Scenario: Import zip from Obsidian with two nested new child
    Given I have a notebook with head note "title 1" and notes:
      | Title   | Details | Parent Title |
      | note 1  |         | title 1      |
      | note 2  |         | note 1       |
    When I Import Obsidian data "import-multiple-layer.zip" to note "title 1"
    Then I should see "title 1/note 1/note 2" with these children
      | note-title |
      | note 3     |
    Then I should see "title 1/note 1" with these children
      | note-title |
      | note 2     |
      | note 4     |
    And I should see note "title 1/note 1/note 2/note 3" has details "content of note 3"
    And I should see note "title 1/note 1/note 4" has details "content of note 4"

   @ignore
  Scenario: Import zip from Obsidian with two nested new child
    Given I have a notebook with head note "title 1" and notes:
      | Title   | Details | Parent Title |
      | note 1  |         | title 1      |
    When I Import Obsidian data "import-two-layer.zip" to note "title 1"
    Then I should see "title 1/note 1/note 2" with these children
      | note-title |
      | note 2     |
      | note 3     |
    And I should see note "title 1/note 1/note 2" has details "content of note 2"
    And I should see note "title 1/note 1/note 2/note 3" has details "content of note 3"

  @ignore
  Scenario: Import zip from Obsidian with new empty folder
    When I Import Obsidian data "import-empty-folder.zip" to note "title 1"
    Then I should see "title 1/note 1" with these children
      | note-title |
      | note 2     |


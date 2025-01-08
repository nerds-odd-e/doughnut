Feature: Notebook export for Obsidian
  Background:
    Given I am logged in as an existing user
    And I have a notebook titled "Medical Notes"
    And the notebook contains the following notes
      | Title           | Content                    |
      | Patient Care    | Basic patient care notes   |
      | Medications     | Common medications list    |
      | Procedures      | Standard procedures guide  |

  Scenario: Export notebook as a flat zip file for Obsidian
    When I go to Notebook page
    And I click on the export for Obsidian option on notebook "Medical Notes"
    Then I should receive a zip file containing
      | Filename           | Format |
      | Patient Care.md    | md     |
      | Medications.md     | md     |
      | Procedures.md      | md     |
    And the zip file should not contain any subdirectories
    And each markdown file should maintain its original content

  @ignore
  Scenario: Export notebook with special characters in title
    When I select the "Medical Notes" notebook
    And I click on the export for Obsidian option
    Then I should receive a zip file with sanitized filenames
    And all markdown files should be at the root level of the zip

  @ignore
  Scenario: Export empty notebook
    When I select the "Medical Notes" notebook
    And I click on the export for Obsidian option
    Then I should receive a zip file containing three files
    And each markdown file should maintain its original content 
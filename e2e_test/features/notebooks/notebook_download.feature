@ignore
Feature: Notebook download for Obsidian
  Background:
    Given I am logged in as an existing user
    And I have a notebook titled "Medical Notes"
    And the notebook contains the following notes
      | Title           | Content                    |
      | Patient Care    | Basic patient care notes   |
      | Medications     | Common medications list    |
      | Procedures      | Standard procedures guide  |

  Scenario: Download notebook as a flat zip file for Obsidian
    When I select the "Medical Notes" notebook
    And I click on the download for Obsidian option
    Then I should receive a zip file containing
      | Filename           | Format |
      | Patient Care.md    | md     |
      | Medications.md     | md     |
      | Procedures.md      | md     |
    And the zip file should not contain any subdirectories
    And each markdown file should maintain its original content

  @ignore
  Scenario: Download notebook with special characters in title
    Given I have a notebook titled "Pediatrics (2024)"
    When I select the "Pediatrics (2024)" notebook
    And I click on the download for Obsidian option
    Then I should receive a zip file with sanitized filenames
    And all markdown files should be at the root level of the zip

  @ignore
  Scenario: Download empty notebook
    Given I have an empty notebook titled "Empty Notes"
    When I select the "Empty Notes" notebook
    And I click on the download for Obsidian option
    Then I should receive an empty zip file
    And I should see a notification that the notebook is empty 

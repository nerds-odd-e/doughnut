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

  @startWithEmptyDownloadsFolder
  # Scenario: Download my own notebook
  #   When I download notebook "Medical Notes"
  #   Then the notebook should be downloaded successfully
  #   And the downloaded file should contain all notes from "Medical Notes"

  Scenario: Download button visibility
    Then I should see a download button for notebook "Medical Notes"
    When I haven't login
    Then I should not see a download button for notebook "Medical Notes" 

Feature: Download Notebook
  As a user
  I want to download my notebook
  So that I can save my notes locally

  Background:
    Given I am logged in as an existing user
    And I have a notebook with head note "Programming Guide" and notes:
      | Title          | Details           | Parent Title     |
      | Python         | A coding language | Programming Guide|
      | Basic Types    | int, str, etc     | Python          |
      | Collections    | lists and dicts   | Python          |

  @startWithEmptyDownloadsFolder
  Scenario: Download my own notebook
    When I download notebook "Programming Guide"
    Then the notebook should be downloaded successfully
    And the downloaded file should contain all notes from "Programming Guide"

  @startWithEmptyDownloadsFolder
  Scenario: Download a shared notebook from bazaar
    Given there is a notebook "JavaScript Basics" by "a_trainer" shared to the Bazaar
    When I download notebook "JavaScript Basics" from the bazaar
    Then the notebook should be downloaded successfully
    And the downloaded file should contain all notes from "JavaScript Basics"

  Scenario: Download button visibility
    Then I should see a download button for notebook "Programming Guide"
    When I haven't login
    Then I should not see a download button for notebook "Programming Guide" 
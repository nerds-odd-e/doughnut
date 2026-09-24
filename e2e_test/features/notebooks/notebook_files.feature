Feature: Notebook files
  As a notebook reader, I want to open a notebook's file on its own page and download it,
  so that I get its exact content under its own name.

  Background:
    Given I am logged in as an existing user
    And I have a notebook "Lab Notebook"
    And the notebook "Lab Notebook" has files:
      | Path                  | Content      |
      | physics/data/run.json | {"speed": 3} |

  Scenario: Open a file and download its exact bytes
    When I jump to the notebook "Lab Notebook"
    And I open the file "run.json" in sidebar folder path "physics/data"
    Then I should see the file page for "run.json" of 12 bytes
    And after reloading, the sidebar is open at folder "data" showing "run.json"
    And downloading the file gives '{"speed": 3}' named "run.json"

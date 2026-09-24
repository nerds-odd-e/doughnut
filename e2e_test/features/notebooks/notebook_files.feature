Feature: Notebook files
  As a notebook reader, I want to see a notebook's files where they live, so that a folder
  holding only files does not look empty.

  Background:
    Given I am logged in as an existing user
    And I have a notebook "Lab Notebook" with notes:
      | Title | Content      | Folder  |
      | Force | Force marker | physics |
    And the notebook "Lab Notebook" uses legacy raw Git attachment storage
    And the notebook "Lab Notebook" has files:
      | Path                  | Content          |
      | physics/force.png     | force image      |
      | physics/data/run.json | {"speed": 3}     |
      | refs/paper.pdf        | paper            |
      | results.csv           | trial,speed      |

  Scenario: Files appear in the sidebar where they live
    When I jump to the notebook "Lab Notebook"
    Then I should see these rows at the sidebar root:
      | physics     |
      | refs        |
      | results.csv |
    And I should see these rows in sidebar folder "physics":
      | data      |
      | Force     |
      | force.png |
    And I should see these rows in sidebar folder "data":
      | run.json |
    And I should see these rows in sidebar folder "refs":
      | paper.pdf |

  Scenario: Open a file and download its exact bytes
    When I jump to the notebook "Lab Notebook"
    And I open the file "run.json" in sidebar folder path "physics/data"
    Then I should see the file page for "run.json" of 12 bytes
    And after reloading, the sidebar is open at folder "data" showing "run.json"
    And downloading the file gives '{"speed": 3}' named "run.json"

  Scenario: LFS file downloads its real bytes
    Given I have a notebook "Figures"
    And the notebook "Figures" has an accepted LFS tip "diagram.png" with payload "diagram-png-bytes" and obsolete payload "old-diagram-bytes"
    When I jump to the notebook "Figures"
    Then I should see these rows at the sidebar root:
      | diagram.png |
    When I open the root file "diagram.png" in the sidebar
    Then I should see the file page for "diagram.png" of 17 bytes
    And downloading the file gives "diagram-png-bytes" named "diagram.png"

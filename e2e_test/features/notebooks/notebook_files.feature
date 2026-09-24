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

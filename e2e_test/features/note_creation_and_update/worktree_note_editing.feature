Feature: Isolated worktree note editing
  As a learner, I want to create and edit a note in this checkout's app
  so that the saved content is still there after reload.

  Background:
    Given I am logged in as an existing user
    And I have a notebook "LeSS training" with notes:
      | Title        | Folder         |
      | Course intro |                |
      | team         | LeSS in Action |

  Scenario: Created note content remains after edit and reload
    When I create a note with title "Re-quirement" under the folder "LeSS in Action" in the notebook "LeSS training"
    And I update note "Re-quirement" with content "Saved in this worktree"
    And I keep the saved note across the other worktree's fixture reset
    And I update note "Re-quirement" with content "Edited after the other worktree reset"
    And I reload the current page for note "Re-quirement"
    Then the note content should include "Edited after the other worktree reset"

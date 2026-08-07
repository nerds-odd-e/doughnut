Feature: Note move
  As a learner, I want to move a note under a folder in another notebook
  so I can reorganize related notes.

  Background:
    Given I am logged in as an existing user
    And I have a notebook "Sedition law" with a note "Sedition"
    And I have a notebook "Sedation care" with notes:
      | Title    | Folder   |
      | Sedation | Sedation |

  @mockBrowserTime
  Scenario: Move a note under a folder and undo
    When I move note "Sedition" under folder "Sedation"
    Then I should see sidebar folder "Sedation" containing these notes:
      | note-title |
      | Sedation   |
      | Sedition   |
    When I undo "move note"
    And I jump to the notebook "Sedation care"
    Then I should see sidebar folder "Sedation" containing these notes:
      | note-title |
      | Sedation   |

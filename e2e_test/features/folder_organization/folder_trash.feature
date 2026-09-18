Feature: Trash and recover a folder
  As a notebook owner, I want to retain a trashed folder subtree
  so that I can revisit and recover it through ordinary Move.

  Background:
    Given I am logged in as an existing user

  Scenario: Trash, revisit, and recover a retained subtree
    Given I have a notebook "Folder Trash NB" with notes:
      | Title           | Content                                      | Folder                           |
      | Topic overview  | ---\nstatus: retained\n---\nTopic body marker | Research/Biology/Topic           |
      | Nested retained | Nested body marker                           | Research/Biology/Topic/Nested    |
      | Active sibling  | Sibling body marker                          | Research/Biology/Active Sibling  |
    And the notebook "Folder Trash NB" has a folder "Empty Nested" under note "Nested retained"
    When I open the folder page for "Topic" in notebook "Folder Trash NB"
    And I type and save the folder readme with text "Topic readme marker"
    And I trash the current folder
    And I reload the folder page
    And I expand folder path "_trash/Research/Biology" in the sidebar
    And I open the folder page at path "_trash/Research/Biology/Topic"
    Then I should see the folder page is in trash
    And the folder readme should contain "Topic readme marker"
    When I move the current folder to notebook root
    Then I should see sidebar folder "Topic"
    When I route to the note "Topic overview"
    Then the note content on the current page should be "Topic body marker"
    When I route to the note "Nested retained"
    Then the note content on the current page should be "Nested body marker"
    And I should see sidebar folder "Empty Nested" under open folder "Nested"
    When I route to the note "Active sibling"
    Then the note content on the current page should be "Sibling body marker"

  Scenario: Keep a colliding trashed folder separate and recover it with its suffix
    Given I have a notebook "Colliding Folder Trash NB" with notes:
      | Title          | Content                 | Folder               |
      | Incoming cells | Incoming identity marker | Biology              |
      | Earlier cells  | Earlier trash marker     | _trash/Biology       |
      | Later cells    | Later trash marker       | _trash/Biology (3)   |
      | Destination    | Destination marker       | Recovered destination |
    When I open the folder page for "Biology" in notebook "Colliding Folder Trash NB"
    And I trash the current folder
    And I reload the folder page
    And I expand folder path "_trash" in the sidebar
    And I open the folder page at path "_trash/Biology (2)"
    Then I should see the folder page is in trash
    When I move the current folder to notebook "Colliding Folder Trash NB" folder "Recovered destination"
    Then the folder page heading should be "Biology (2)"
    When I route to the note "Incoming cells"
    Then the note content on the current page should be "Incoming identity marker"
    When I route to the note "Earlier cells"
    Then the note content on the current page should be "Earlier trash marker"
    When I route to the note "Later cells"
    Then the note content on the current page should be "Later trash marker"

  Scenario: Permanently delete a folder that is in trash
    Given I have a notebook "Trash Cleanup NB" with notes:
      | Title      | Content      | Folder                |
      | Cells      | Cells marker | _trash/Biology        |
      | Deep cells | Deep marker  | _trash/Biology/Nested |
      | Kept cells | Kept marker  | _trash/Chemistry      |
    When I open the folder page for "Biology" in notebook "Trash Cleanup NB"
    Then I should see the folder page is in trash
    When I permanently delete the current folder
    And I expand folder path "_trash" in the sidebar
    Then I should not see sidebar folder "Biology"
    And I should not see sidebar folder "Nested"
    And I should see sidebar folder "Chemistry" containing these notes:
      | note-title |
      | Kept cells |

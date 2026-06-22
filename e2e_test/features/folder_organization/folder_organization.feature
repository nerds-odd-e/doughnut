Feature: Folder organization
  As a learner, I want to move folders within a notebook
  so that my hierarchy matches how I think about the material.

  Background:
    Given I am logged in as an existing user
    And I have a notebook "Organize NB" with notes:
      | Title     | Folder |
      | Root note |        |
      | In folder | Alpha  |

  Scenario: Move a nested folder to notebook root from the folder page
    When I create a folder named "Beta" while viewing note "In folder"
    Then I should see sidebar folder "Beta" under open folder "Alpha"
    When I activate folder "Beta" in the sidebar
    And I move folder "Beta" to notebook root using the folder page
    Then I should see sidebar folder "Beta"

  Scenario: Sibling name clash blocks a folder move and shows inline error
    Given I have a notebook "Organize NB" with notes:
      | Title       | Folder      |
      | Root Beta   | Beta        |
      | Nested Beta | Alpha/Beta  |
    When I view note "Nested Beta"
    And I activate folder "Beta" under the open folder "Alpha" in the sidebar
    And I attempt to move folder "Beta" under "Alpha" to notebook root using the folder page
    Then the folder page shows error "A folder with this name already exists here."

  Scenario: Dissolve a folder, promoting its notes and subfolders to the parent
    Given I have a notebook "Organize NB" with notes:
      | Title | Folder          |
      | Loose | Outer/Mid       |
      | Deep  | Outer/Mid/Inner |
    When I view note "Loose"
    And I activate folder "Mid" under the open folder "Outer" in the sidebar
    And I dissolve folder "Mid" under "Outer" using the folder page
    Then I should see sidebar folder "Inner" under open folder "Outer"
    And I should see note "Loose" under open folder "Outer"

  Scenario: Moving a folder into a same-name destination merges them when confirmed
    Given I have a notebook "Organize NB" with notes:
      | Title       | Folder      |
      | Root note A | Shared      |
      | Inner note  | Alpha/Shared |
    When I view note "Inner note"
    And I activate folder "Shared" under the open folder "Alpha" in the sidebar
    And I move folder "Shared" under "Alpha" to notebook root and confirm merge using the folder page
    Then I should see note "Root note A" under open folder "Shared"
    And I should see note "Inner note" under open folder "Shared"

  Scenario: Dissolving a folder merges promoted subfolders when confirmed
    Given I have a notebook "Organize NB" with notes:
      | Title      | Folder          |
      | Outer note | Outer/Inner     |
      | Mid note   | Outer/Mid/Inner |
    When I view note "Mid note"
    And I activate folder "Mid" under the open folder "Outer" in the sidebar
    And I dissolve folder "Mid" under "Outer" and confirm merge using the folder page
    Then I should see note "Outer note" under open folder "Inner"
    And I should see note "Mid note" under open folder "Inner"

  Scenario: Move a folder using search when the destination is not in quick picks
    Given I have a notebook "Organize NB" with notes:
      | Title | Folder     |
      | n1    | Alpha/Beta |
      | n2    | Gamma      |
    When I view note "n1"
    And I activate folder "Beta" under the open folder "Alpha" in the sidebar
    And I move folder "Beta" under "Alpha" to folder "Gamma" using folder search on the folder page
    Then I should see sidebar folder "Beta" under collapsed folder "Gamma"

  @mockBrowserTime
  Scenario: Moving a folder to another notebook root keeps boundary wiki links correct
    Given I have a notebook "FolderXMove Old NB" with notes:
      | Title   | Content                 | Folder                   |
      | Target  | old notebook target     | FolderXMove Root         |
      | Carrier | Read [[Target]].        | FolderXMove Root/Moved   |
      | Ref     | See [[Carrier]].        | FolderXMove Root         |
    And I have a notebook "FolderXMove New NB" with a note "Placeholder" and content "dest"
    When I view note "Carrier"
    And I activate folder "Moved" under the open folder "FolderXMove Root" in the sidebar
    And I move folder "Moved" under "FolderXMove Root" to notebook "FolderXMove New NB" root using the folder page
    Then I should see sidebar folder "Moved"
    When I route to the note "Carrier"
    And I view the note content as markdown
    Then the note content markdown source should contain "[[FolderXMove Old NB:Target|Target]]"
    When I route to the note "Ref"
    And I view the note content as markdown
    Then the note content markdown source should contain "[[FolderXMove New NB:Carrier|Carrier]]"
    When I route to the note "Carrier"
    And I view the note content as rich content
    Then the link "Target" should open the note titled "Target"
    And the note content on the current page should be "old notebook target"

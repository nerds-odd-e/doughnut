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

  Scenario: Move a folder using search when the destination is not in quick picks
    Given I have a notebook "Organize NB" with notes:
      | Title | Folder     |
      | n1    | Alpha/Beta |
      | n2    | Gamma      |
    When I view note "n1"
    And I activate folder "Beta" under the open folder "Alpha" in the sidebar
    And I move folder "Beta" under "Alpha" to folder "Gamma" using folder search on the folder page
    Then I should see sidebar folder "Beta" under collapsed folder "Gamma"

Feature: Folder organization
  As a learner, I want to move folders within a notebook
  so that my hierarchy matches how I think about the material.

  Background:
    Given I am logged in as an existing user
    And I have a notebook "Organize NB" with notes:
      | Title     | Folder |
      | Root note |        |
      | In folder | Alpha  |

  Scenario: Move a nested folder to notebook root from the sidebar
    When I create a folder named "Beta" while viewing note "In folder"
    Then I should see sidebar folder "Beta" under folder "Alpha"
    When I activate folder "Beta" in the sidebar
    And I move the active folder to notebook root using the sidebar folder dialog
    Then I should see sidebar folder "Beta"
    And I should not see sidebar folder "Beta" under folder "Alpha"

  Scenario: Sibling name clash blocks a folder move and shows inline error
    Given I have a notebook "Organize NB" with notes:
      | Title       | Folder      |
      | Root Beta   | Beta        |
      | Nested Beta | Alpha/Beta  |
    When I view note "Nested Beta"
    And I activate folder "Beta" under folder "Alpha" in the sidebar
    And I attempt to move the active folder to notebook root using the sidebar folder dialog
    Then the sidebar folder dialog shows error "A folder with this name already exists here."

  Scenario: Dissolve a folder, promoting its notes and subfolders to the parent
    Given I have a notebook "Organize NB" with notes:
      | Title | Folder          |
      | Loose | Outer/Mid       |
      | Deep  | Outer/Mid/Inner |
    When I view note "Loose"
    And I activate folder "Mid" under folder "Outer" in the sidebar
    And I dissolve the active folder using the sidebar folder dialog
    Then I should not see sidebar folder "Mid" under folder "Outer"
    And I should see sidebar folder "Inner" under folder "Outer"
    And I should see note "Loose" under folder "Outer"

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

  Scenario: Folder organize control is hidden without an active sidebar folder
    When I open the notebook "Organize NB" from my notebooks catalog
    And I open the note "Root note" from the sidebar
    Then the folder organize control should not be visible in the sidebar

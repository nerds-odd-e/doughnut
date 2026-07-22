Feature: Note, folder, and notebook surface landmarks
  As a learner, I want note, folder, and notebook pages to stay visually distinct
  so that I always know whether I am editing a document, administering a folder, or on the notebook readme.

  Background:
    Given I am logged in as an existing user
    And I have a notebook "Surface landmarks suite" with notes:
      | Title         | Folder |
      | Surface note  | Alpha  |

  Scenario: Notebook, folder, and note main columns use different landmarks
    When I open the notebook "Surface landmarks suite" from my notebooks catalog
    Then the notebook workspace readme shows name "Surface landmarks suite" and readme
    And notebook Readme and Settings tabs are present
    And the note document toolbar is not present
    And the folder admin controls are not present

    When I view note "Surface note"
    And I open the folder page for "Alpha" from the sidebar
    Then folder Readme and Settings tabs are present
    And the folder admin controls are present
    And notebook Readme and Settings tabs are not present
    And the note document toolbar is not present

    When I view note "Surface note"
    Then the note document toolbar is present
    And notebook Readme and Settings tabs are not present
    And the folder admin controls are not present

Feature: Folder page index
  As a learner, I want a folder landing page with an editable index note
  so that folder-level context persists like the notebook index.

  Background:
    Given I am logged in as an existing user
    And I have a notebook "Folder Index NB" with notes:
      | Title    | Folder |
      | In Alpha | Alpha  |

  Scenario: Folder index lazy create persists after reload
    When I view note "In Alpha"
    And I open the folder page for "Alpha" from the sidebar
    And I type and save the folder index with text "Persistent folder landing"
    Then the folder index should contain "Persistent folder landing"
    When I reload the folder page
    Then the folder index should contain "Persistent folder landing"

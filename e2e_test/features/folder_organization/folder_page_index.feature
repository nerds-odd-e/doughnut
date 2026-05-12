Feature: Folder page index
  As a learner, I want a folder landing page with editable index content
  so that folder-level context persists like the notebook index.

  Background:
    Given I am logged in as an existing user
    And I have a notebook "Folder Index NB" with notes:
      | Title    | Folder |
      | In Alpha | Alpha  |

  Scenario: Folder index content persists after reload
    When I view note "In Alpha"
    And I open the folder page for "Alpha" from the sidebar
    And I type and save the folder index with text "Persistent folder landing"
    Then the folder index should contain "Persistent folder landing"
    When I reload the folder page
    Then the folder index should contain "Persistent folder landing"

  Scenario: New note from folder page uses folder index title_pattern default
    When I view note "In Alpha"
    And I open the folder page for "Alpha" from the sidebar
    When I add a rich note property with key "title_pattern" and value "{{date}}"
    And I type and save the folder index with text "Scoped index marker"
    When I create a new note from the sidebar submitting the default title

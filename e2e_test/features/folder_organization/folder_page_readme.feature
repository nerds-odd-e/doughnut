Feature: Folder page readme
  As a learner, I want a folder landing page with editable readme content
  so that folder-level context persists like the notebook readme.

  Background:
    Given I am logged in as an existing user
    And I have a notebook "Folder Readme NB" with notes:
      | Title    | Folder |
      | In Alpha | Alpha  |

  Scenario: Folder readme content persists after reload
    When I open the folder page for "Alpha" in notebook "Folder Readme NB"
    And I type and save the folder readme with text "Persistent folder landing"
    Then the folder readme should contain "Persistent folder landing"
    When I reload the folder page
    Then the folder readme should contain "Persistent folder landing"

  Scenario: New note from folder page uses folder readme title_pattern default
    When I view note "In Alpha"
    And I open the folder page for "Alpha" from the sidebar
    When I add a rich note property with key "title_pattern" and value "{{date}}"
    And I type and save the folder readme with text "Scoped readme marker"
    When I create a new note from the sidebar submitting the default title

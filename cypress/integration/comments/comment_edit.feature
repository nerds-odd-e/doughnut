Feature: Edit an existing comment from a note
  As a user, I want to edit an existing comment from an existing note

  Background:
    Given I've logged in as an existing user
    And there are some notes for the current user
      | title          | testingParent  | description         | author |
      | LeSS in Action |                | An awesome training | a      |
      | team           | LeSS in Action |                     | b      |
      | tech           | LeSS in Action |                     | c      |

  @ignore @featureToggle
  Scenario: Edit a comment from a note

    When I open "LeSS in Action" note from top level
    And I add a comment with description "edit"
    And I edit the "edit" comment with description "comment to be edited"
    Then I should see comment with value "comment to be edited"

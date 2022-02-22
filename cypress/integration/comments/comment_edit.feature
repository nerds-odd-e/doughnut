Feature: Edit an existing comment from a note
  As a user, I want to edit an existing comment from an existing note

  Background:
    Given I've logged in as an existing user
    And there are some notes for the current user
      | title          | testingParent  | description         | author |
      | LeSS in Action |                | An awesome training | a      |
      | team           | LeSS in Action |                     | b      |
      | tech           | LeSS in Action |                     | c      |

  @ignore
  Scenario: Remove a comment from a note

    When I select my note with title 'LeSS in Action'
    And I click the add comment button
    Then I should see a new comment input box displayed
    And I input 'comment to be edited' in the comment input box
    And I click outside the input box
    Then I should see comment added to note
    And I click the edit comment button on the comment with value "comment to be edited"
    And I input ' testing edit' in the comment input box
    And I click outside the input box
    Then I should see comment with value "comment to be edited testing edit"

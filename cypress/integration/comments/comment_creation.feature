Feature: Add a new comment to a note
  As a user, I want to add a new comment to an existing note

  Background:
    Given I've logged in as an existing user
    And there are some notes for the current user
      | title          | testingParent  | description         | author |
      | LeSS in Action |                | An awesome training | a      |
      | team           | LeSS in Action |                     | b      |
      | tech           | LeSS in Action |                     | c      |

  @ignore
  Scenario: Add a new comment to my note
    
    When I select my note with title 'LeSS in Action'
    And I click the add comment button
    Then I should see a new comment input box displayed
    And I input 'some comment' in the comment input box 
    And I click outside the input box
    Then I should see comment added to note


  @ignore
  Scenario: Add a new comment to another person's note
    
    When I select user 'a' note with title 'team'
    And I click the add comment button
    Then I should see a new comment input box displayed
    And I input 'some comment' in the comment input box 
    And I click outside the input box
    Then I should see comment added to note
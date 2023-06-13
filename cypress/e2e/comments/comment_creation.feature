@ignore
Feature: Comment creation
  As a learner, I want to add partially formed thoughts as comments to my notes.

  Background:
    Given I've logged in as an existing user
    And there are some notes for the current user
      | title          | description         |
      | LeSS in Action | An awesome training |

  Scenario: Create a comment belonging to the current note
    When I create a comment belonging to "LeSS in Action":
      |                         |
      | Re-think the way we do requirement |
    And I should see "My Notes/LeSS in Action" with these children
      | note-title   |
      | team         |
      | tech         |
      | Re-quirement |

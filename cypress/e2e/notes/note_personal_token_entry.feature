Feature: Personal token entry for Open AI functionality
  As a learner, I want to use my personal token to utilize the Open AI features for getting suggestions on this note

  Background:
    Given I've logged in as an existing user
    And there are some notes for the current user
      | title          | testingParent  | description         |
      | LeSS in Action |                | An awesome training |
      | team           | LeSS in Action |                     |
      | tech           | LeSS in Action |                     |

  @ignore
  Scenario: User does not have a personal token registered
    When I click the suggest button for an existing note:
      | Title        | Description                        |
      | Re-quirement | Re-think the way we do requirement |
    Then A modal opens which prompts entry of the personal token
    When I enter my personal token "tokenValue"
    And I click the submit button
    Then My personal token is registered to be used by the system

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
  Scenario: User should be able to use personal token for open ai related service
    Given An external user without the personal open ai token secret in the session storage
    When the User triggers use of OpenAI service
    Then the user is presented with a modal to provide their own personal token
    And the user is able to use open ai services (open)

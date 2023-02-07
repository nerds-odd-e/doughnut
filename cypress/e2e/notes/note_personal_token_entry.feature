Feature: Personal token entry for Open AI functionality
  As a learner, I want to use my personal token to utilize the OpenAI features for getting suggestions on this note

  Background:
    Given I've logged in as an existing user
    And there are some notes for the current user
      | title          |
      | Flowers        |

  @ignore
  Scenario: User should be able to use personal token for openAI related service
    Given I have a personal openAI token "sk-validtoken"
    And OpenAI thinks that "Flowers" means "Flowers are beautiful creations of the earth"
    When I trigger use of get suggestion of "Flowers" with openAI token "sk-validtoken"
    Then I should get a suggestion "Flowers are beautiful creations of the earth"

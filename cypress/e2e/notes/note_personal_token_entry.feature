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
    #implementation of modal

    And OpenAI thinks that "Flowers" means "Flowers are beautiful creations of the earth"
    #mocked return from OpenAI

    When I trigger use of get suggestion of "Flowers" with openAI token "sk-validtoken"
    #click the Suggest button

    Then I should get a suggestion "Flowers are beautiful creations of the earth"
    #expected return from OpenAI (mocked in the And above)

    @ignore
    Scenario Outline: get suggest with open AI personal or fallback token

      Examples:

     | fallback key | personal key | result  |
     | valid key    | valid key    | works   |
     | invalid key  | valid key    | works   |
     | valid key    | invalid      | works   |
     | invalid      | invalid      | no work |

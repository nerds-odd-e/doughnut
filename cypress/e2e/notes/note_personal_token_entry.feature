Feature: Personal token entry for Open AI functionality
  As a learner, I want to use my personal token to utilize the OpenAI features for getting suggestions on this note

  Background:
    Given I've logged in as an existing user
    And there are some notes for the current user
      | title          |
      | Flowers        |
    And OpenAI thinks that "Flowers" means "Flowers are beautiful creations of the earth"


    @ignore
    Scenario Outline: get suggest with open AI personal or fallback token
      Given the fallback key of the system is <fallback key>
      And I have a personal openAI token <personal key>
      When I trigger use of get suggestion of "Flowers"
      Then I should get a suggestion "Flowers are beautiful creations of the earth": <result>

      Examples:
     | fallback key | personal key | result  |
     | valid key    | valid key    | work    |
     | invalid key  | valid key    | work    |
     | valid key    | invalid      | work    |
     | invalid      | invalid      | no work |
